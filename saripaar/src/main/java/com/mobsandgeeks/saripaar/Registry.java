/*
 * Copyright (C) 2014 Mobs & Geeks
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobsandgeeks.saripaar;

import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.mobsandgeeks.saripaar.adapter.CheckBoxBooleanAdapter;
import com.mobsandgeeks.saripaar.adapter.RadioButtonBooleanAdapter;
import com.mobsandgeeks.saripaar.adapter.SpinnerIndexAdapter;
import com.mobsandgeeks.saripaar.adapter.TextViewDoubleAdapter;
import com.mobsandgeeks.saripaar.adapter.TextViewFloatAdapter;
import com.mobsandgeeks.saripaar.adapter.TextViewIntegerAdapter;
import com.mobsandgeeks.saripaar.adapter.TextViewStringAdapter;
import com.mobsandgeeks.saripaar.adapter.ViewDataAdapter;
import com.mobsandgeeks.saripaar.annotation.ValidateUsing;
import com.mobsandgeeks.saripaar.exception.SaripaarViolationException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Maintains a registry of all {@link android.view.View}s and
 * {@link com.mobsandgeeks.saripaar.adapter.ViewDataAdapter}s that are registered to rule
 * {@link java.lang.annotation.Annotation}s.
 *
 * @author Ragunath Jawahar {@literal <rj@mobsandgeeks.com>}
 * @since 2.0
 */
final class Registry {
    public static final String TAG = "Registry";
    private static final Map<Class<? extends View>, HashMap<Class<?>, ViewDataAdapter>> STOCK_ADAPTERS = new HashMap();
    private Map<Class<? extends Annotation>, HashMap<Class<? extends View>, ViewDataAdapter>> mMappings = new HashMap();

    Registry() {
    }

    public void register(Class<? extends Annotation>... ruleAnnotations) {
        Class[] var2 = ruleAnnotations;
        int var3 = ruleAnnotations.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            Class<? extends Annotation> ruleAnnotation = var2[var4];
            this.assertIsValidRuleAnnotation(ruleAnnotation);
            ValidateUsing validateUsing = (ValidateUsing)ruleAnnotation.getAnnotation(ValidateUsing.class);
            Class<?> ruleDataType = Reflector.getRuleDataType(validateUsing);
            HashMap<Class<?>, ViewDataAdapter> viewDataAdapterMap = (HashMap)STOCK_ADAPTERS.get(TextView.class);
            if (viewDataAdapterMap != null) {
                ViewDataAdapter dataAdapter = (ViewDataAdapter)viewDataAdapterMap.get(ruleDataType);
                if (dataAdapter == null) {
                    String message = String.format("Unable to find a matching adapter for `%s`, that returns a `%s`.", ruleAnnotation.getName(), ruleDataType.getName());
                    throw new SaripaarViolationException(message);
                }

                this.register(TextView.class, ruleDataType, dataAdapter, ruleAnnotation);
            }
        }

    }

    public <VIEW extends View, DATA_TYPE> void register(Class<VIEW> viewType, Class<DATA_TYPE> ruleDataType, ViewDataAdapter<VIEW, DATA_TYPE> viewDataAdapter, Class<? extends Annotation>... ruleAnnotations) {
        if (ruleAnnotations != null && ruleAnnotations.length > 0) {
            Class[] var5 = ruleAnnotations;
            int var6 = ruleAnnotations.length;

            for(int var7 = 0; var7 < var6; ++var7) {
                Class<? extends Annotation> ruleAnnotation = var5[var7];
                this.register(ruleAnnotation, ruleDataType, viewType, viewDataAdapter);
            }
        }

    }

    public Set<Class<? extends Annotation>> getRegisteredAnnotations() {
        return this.mMappings.keySet();
    }

    public <VIEW extends View> ViewDataAdapter<VIEW, ?> getDataAdapter(Class<? extends Annotation> annotationType, Class<VIEW> viewType) {
        HashMap<Class<? extends View>, ViewDataAdapter> viewDataAdapterHashMap = (HashMap)this.mMappings.get(annotationType);
        ViewDataAdapter matchingViewAdapter = null;
        if (viewDataAdapterHashMap != null) {
            matchingViewAdapter = (ViewDataAdapter)viewDataAdapterHashMap.get(viewType);
            if (matchingViewAdapter == null) {
                matchingViewAdapter = this.getCompatibleViewDataAdapter(viewDataAdapterHashMap, viewType);
            }
        }

        return matchingViewAdapter;
    }

    private <VIEW extends View, DATA_TYPE> void register(Class<? extends Annotation> ruleAnnotation, Class<DATA_TYPE> ruleDataType, Class<VIEW> view, ViewDataAdapter<VIEW, DATA_TYPE> viewDataAdapter) {
        this.assertIsValidRuleAnnotation(ruleAnnotation);
        this.assertCompatibleReturnType(ruleDataType, viewDataAdapter);
        HashMap viewAdapterPairs;
        if (this.mMappings.containsKey(ruleAnnotation)) {
            viewAdapterPairs = (HashMap)this.mMappings.get(ruleAnnotation);
        } else {
            viewAdapterPairs = new HashMap();
            this.mMappings.put(ruleAnnotation, viewAdapterPairs);
        }

        if (viewAdapterPairs.containsKey(view)) {
            String message = String.format("A '%s' for '%s' has already been registered.", ruleAnnotation.getName(), view.getName());
            Log.w("Registry", message);
        } else {
            viewAdapterPairs.put(view, viewDataAdapter);
        }

    }

    private void assertIsValidRuleAnnotation(Class<? extends Annotation> ruleAnnotation) {
        boolean validRuleAnnotation = Reflector.isAnnotated(ruleAnnotation, ValidateUsing.class);
        if (!validRuleAnnotation) {
            String message = String.format("'%s' is not annotated with '%s'.", ruleAnnotation.getName(), ValidateUsing.class.getName());
            throw new IllegalArgumentException(message);
        } else {
            this.assertAttribute(ruleAnnotation, "sequence", Integer.TYPE);
            this.assertAttribute(ruleAnnotation, "message", String.class);
            this.assertAttribute(ruleAnnotation, "messageResId", Integer.TYPE);
        }
    }

    private void assertAttribute(Class<? extends Annotation> annotationType, String attributeName, Class<?> attributeType) {
        Method attributeMethod = Reflector.getAttributeMethod(annotationType, attributeName);
        if (attributeMethod == null) {
            String message = String.format("'%s' requires the '%s' attribute.", annotationType.getName(), attributeName);
            throw new SaripaarViolationException(message);
        } else {
            Class<?> returnType = attributeMethod.getReturnType();
            if (!attributeType.equals(returnType)) {
                String message = String.format("'%s' in '%s' should be of type '%s', but was '%s'.", attributeName, annotationType.getName(), attributeType.getName(), returnType.getName());
                throw new SaripaarViolationException(message);
            }
        }
    }

    private <DATA_TYPE, VIEW extends View> void assertCompatibleReturnType(Class<DATA_TYPE> ruleDataType, ViewDataAdapter<VIEW, DATA_TYPE> viewDataAdapter) {
        Method getDataMethod = Reflector.findGetDataMethod(viewDataAdapter.getClass());
        Class<?> adapterReturnType = getDataMethod.getReturnType();
        if (!ruleDataType.equals(adapterReturnType)) {
            String message = String.format("'%s' returns '%s', but expecting '%s'.", viewDataAdapter.getClass().getName(), adapterReturnType.getName(), ruleDataType.getName());
            throw new IllegalArgumentException(message);
        }
    }

    private <VIEW extends View> ViewDataAdapter getCompatibleViewDataAdapter(HashMap<Class<? extends View>, ViewDataAdapter> viewDataAdapterHashMap, Class<VIEW> viewType) {
        ViewDataAdapter compatibleViewAdapter = null;
        Set<Class<? extends View>> registeredViews = viewDataAdapterHashMap.keySet();
        Iterator var5 = registeredViews.iterator();

        while(var5.hasNext()) {
            Class<? extends View> registeredView = (Class)var5.next();
            if (registeredView.isAssignableFrom(viewType)) {
                compatibleViewAdapter = (ViewDataAdapter)viewDataAdapterHashMap.get(registeredView);
            }
        }

        return compatibleViewAdapter;
    }

    static {
        HashMap<Class<?>, ViewDataAdapter> adapters = new HashMap();
        adapters.put(Boolean.class, new CheckBoxBooleanAdapter());
        STOCK_ADAPTERS.put(CheckBox.class, adapters);
        adapters = new HashMap();
        adapters.put(Boolean.class, new RadioButtonBooleanAdapter());
        STOCK_ADAPTERS.put(RadioButton.class, adapters);
        adapters = new HashMap();
        adapters.put(Integer.class, new SpinnerIndexAdapter());
        STOCK_ADAPTERS.put(Spinner.class, adapters);
        adapters = new HashMap();
        adapters.put(String.class, new TextViewStringAdapter());
        adapters.put(Integer.class, new TextViewIntegerAdapter());
        adapters.put(Float.class, new TextViewFloatAdapter());
        adapters.put(Double.class, new TextViewDoubleAdapter());
        STOCK_ADAPTERS.put(TextView.class, adapters);
    }
}