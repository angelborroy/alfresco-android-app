package org.alfresco.mobile.android.application.configuration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.mobile.android.api.model.Folder;
import org.alfresco.mobile.android.api.model.ModelDefinition;
import org.alfresco.mobile.android.api.model.Node;
import org.alfresco.mobile.android.api.model.Property;
import org.alfresco.mobile.android.api.model.config.ConfigConstants;
import org.alfresco.mobile.android.api.model.config.ConfigScope;
import org.alfresco.mobile.android.api.model.config.FieldConfig;
import org.alfresco.mobile.android.api.model.config.FieldGroupConfig;
import org.alfresco.mobile.android.api.model.config.FormConfig;
import org.alfresco.mobile.android.api.services.ConfigService;
import org.alfresco.mobile.android.application.R;
import org.alfresco.mobile.android.application.ui.form.BaseField;
import org.alfresco.mobile.android.application.ui.form.FieldTypeFactory;
import org.alfresco.mobile.android.application.ui.form.FieldTypeRegistry;
import org.alfresco.mobile.android.application.ui.form.PathField;
import org.alfresco.mobile.android.application.ui.form.views.AlfrescoFieldView;
import org.alfresco.mobile.android.async.OperationEvent;
import org.alfresco.mobile.android.async.Operator;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FormConfigManager extends BaseConfigManager
{
    public static final String CLEAR_VALUE = "**clear";

    private ModelDefinition typeDefinition;

    private Node node;

    private Folder parentFolder;

    private Map<String, BaseField> fieldsIndex;

    private HashMap<String, Serializable> currentValues;

    private List<String> modelRequested = new ArrayList<>();

    private HashMap<String, RequestHolder> operationFieldIndex = new HashMap<>();

    private Fragment fr;

    // ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS & HELPERS
    // ///////////////////////////////////////////////////////////////////////////
    public FormConfigManager(Fragment fr, ConfigService configService, ViewGroup vRoot)
    {
        super(fr.getActivity(), configService);
        this.fr = fr;
        this.vRoot = vRoot;
    }

    // ///////////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    // ///////////////////////////////////////////////////////////////////////////
    public boolean displayProperties(Node node, Folder parentFolder)
    {
        try
        {
            this.node = node;
            this.parentFolder = parentFolder;

            vRoot.removeAllViews();
            View v = generateProperties(ConfigConstants.VIEW_NODE_PROPERTIES, LayoutInflater.from(getActivity()), false);
            vRoot.addView(v);
        }
        catch (Exception e)
        {
            Log.d("FormConfig", Log.getStackTraceString(e));
            return false;
        }
        return true;
    }

    public View displayEditForm(ModelDefinition typeDefinition, Node node, HashMap<String, Serializable> currentValue)
    {
        try
        {
            this.node = node;
            this.typeDefinition = typeDefinition;
            this.currentValues = currentValue;
            return generateProperties(ConfigConstants.VIEW_EDIT_PROPERTIES, LayoutInflater.from(getActivity()), true);
        }
        catch (Exception e)
        {
            Log.d("FormConfig", Log.getStackTraceString(e));
            return null;
        }
    }

    public HashMap<String, Serializable> prepareProperties()
    {
        HashMap<String, Serializable> props = new HashMap<String, Serializable>(fieldsIndex.size());
        for (Entry<String, BaseField> entry : fieldsIndex.entrySet())
        {
            if (entry.getValue().hasChanged())
            {
                props.put(entry.getKey(), (Serializable) entry.getValue().getOutputValue());
            }
        }
        return props;
    }

    public void setPropertyValue(String propertyId, Object object)
    {
        fieldsIndex.get(propertyId).setPropertyValue(object);
    }

    public Object getValuePicked(String propertyId)
    {
        return fieldsIndex.get(propertyId).getValuePicked();
    }

    public BaseField getField(String fieldId)
    {
        return fieldsIndex.get(fieldId);
    }

    public void setOperationData(String requestId, OperationEvent event)
    {
        if (!operationFieldIndex.containsKey(requestId)) { return; }

        // TODO Display Error Message
        if (operationFieldIndex.containsKey(requestId) && event.hasException) { return; }

        RequestHolder holder = operationFieldIndex.get(requestId);
        holder.field.setOperationData(event);

        // Update Index
        operationFieldIndex.remove(requestId);
        modelRequested.remove(holder.modelId);
    }

    // ///////////////////////////////////////////////////////////////////////////
    // DRIVEN BY OBJECT
    // ///////////////////////////////////////////////////////////////////////////
    protected View generateProperties(String formIdentifier, LayoutInflater li, boolean isEdition)
    {
        ViewGroup rootView = (ViewGroup) li.inflate(R.layout.form_root, null);
        ViewGroup hookView = rootView;
        if (isEdition)
        {
            fieldsIndex = new HashMap<String, BaseField>();
        }

        // Retrieve form config by Id
        ConfigScope scope = configManager.getCurrentScope();
        scope.add(ConfigScope.NODE, node);
        FormConfig config = configService.getFormConfig(formIdentifier, scope);

        if (config.getGroups() != null && config.getGroups().size() > 0)
        {
            // Header
            if (!TextUtils.isEmpty(config.getLabel()))
            {
                ViewGroup headerView = (ViewGroup) li.inflate(R.layout.form_header, null);
                TextView tv = (TextView) headerView.findViewById(R.id.title);
                tv.setText(config.getLabel());
                hookView = (ViewGroup) headerView.findViewById(R.id.group_panel);
                rootView.addView(headerView);
            }

            // Add Children
            for (FieldGroupConfig group : config.getGroups())
            {
                createPropertyFields(group, hookView, li, isEdition);
            }
        }

        return rootView;
    }

    // ///////////////////////////////////////////////////////////////////////////
    // UI GENERATOR
    // ///////////////////////////////////////////////////////////////////////////
    private void createPropertyFields(FieldGroupConfig group, ViewGroup hookView, LayoutInflater li, boolean isEdition)
    {
        ViewGroup groupview = hookView;

        // Header
        if (!TextUtils.isEmpty(group.getLabel()))
        {
            ViewGroup grouprootview = (ViewGroup) li.inflate(R.layout.form_header, null);
            TextView tv = (TextView) grouprootview.findViewById(R.id.title);
            tv.setText(group.getLabel());
            groupview = (ViewGroup) grouprootview.findViewById(R.id.group_panel);
            hookView.addView(grouprootview);
        }

        // For each properties, display the line associated
        for (FieldConfig fieldConfig : group.getItems())
        {
            if (fieldConfig instanceof FieldGroupConfig)
            {
                createPropertyFields((FieldGroupConfig) fieldConfig, groupview, li, isEdition);
                continue;
            }

            // Retrieve the Field builder based on config
            Property nodeProp = node.getProperty(fieldConfig.getModelIdentifier());
            View fieldView = null;
            BaseField field = createField(nodeProp, fieldConfig, typeDefinition);
            if (field == null)
            {
                continue;
            }

            // View Generation depending on State : Edition or Read
            if (isEdition)
            {
                // Log.d("Form", fieldConfig.getModelIdentifier() + " " +
                // field.getClass());
                fieldView = field.getEditView(typeDefinition, groupview);
                if (currentValues != null)
                {
                    String value = currentValues.get(fieldConfig.getModelIdentifier()) == null ? "" : currentValues
                            .get(fieldConfig.getModelIdentifier()).toString();
                    ((AlfrescoFieldView) fieldView).setText(value);
                }
                fieldsIndex.put(fieldConfig.getModelIdentifier(), field);
            }
            else if (nodeProp != null && nodeProp.getValue() != null)
            {
                fieldView = field.createReadableView();
            }
            else if (field.requireAsync() || field.requireExtra())
            {
                fieldView = field.createReadableView();
            }

            // If a view has been generated we kept it.
            if (fieldView != null)
            {
                groupview.addView(fieldView);
            }

            // If requires fragment for pickers.
            if (field.requiresPicker())
            {
                field.initPicker(fr);
            }

            // If requires Async
            if (field.requireAsync() && !modelRequested.contains(fieldConfig.getModelIdentifier()))
            {
                String requestId = Operator.with(getActivity()).load(field.requestData(node));
                RequestHolder holder = new RequestHolder(field, fieldConfig.getModelIdentifier(), requestId);
                modelRequested.add(fieldConfig.getModelIdentifier());
                operationFieldIndex.put(requestId, holder);
            }

            // TODO Generalize extra information required. For the moment it's
            // tight to parentFolder.
            if (field.requireExtra())
            {
                Bundle b = new Bundle();
                b.putSerializable(PathField.EXTRA_PARENT_FOLDER, parentFolder);
                field.setExtra(b);
            }
        }
    }

    protected BaseField createField(Property property, FieldConfig fieldConfig, ModelDefinition typeDefinition)
    {
        BaseField fieldManager = null;
        // 3 use cases
        if (!TextUtils.isEmpty(fieldConfig.getType()))
        {
            // Field type has been defined inside the configuration
            fieldManager = FieldTypeFactory.createFieldControlType(getActivity(), property, fieldConfig);
        }
        else if (typeDefinition != null && typeDefinition.getPropertyDefinition(fieldConfig.getIdentifier()) != null)
        {
            // Field type is not defined by the configuration
            // We generate the field based on its propertyType & Definition
            String fieldType = FieldTypeRegistry.getFieldType(typeDefinition, fieldConfig.getIdentifier());
            fieldManager = FieldTypeFactory.createFieldControlType(getActivity(), fieldType, property, fieldConfig);
        }
        else
        {
            // By default it's a TextField and value is a string
            fieldManager = FieldTypeFactory.createFieldControlType(getActivity(), FieldTypeRegistry.DEFAULT_FIELD,
                    property, fieldConfig);
        }

        return fieldManager;
    }

    private static class RequestHolder
    {
        public final BaseField field;

        public final String modelId;

        public final String requestId;

        public RequestHolder(BaseField field, String modelId, String requestId)
        {
            this.field = field;
            this.modelId = modelId;
            this.requestId = requestId;
        }
    }
}
