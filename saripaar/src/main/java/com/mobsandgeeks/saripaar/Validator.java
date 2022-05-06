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

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.mobsandgeeks.saripaar.adapter.CheckBoxBooleanAdapter;
import com.mobsandgeeks.saripaar.adapter.RadioButtonBooleanAdapter;
import com.mobsandgeeks.saripaar.adapter.SpinnerIndexAdapter;
import com.mobsandgeeks.saripaar.adapter.ViewDataAdapter;
import com.mobsandgeeks.saripaar.annotation.AssertFalse;
import com.mobsandgeeks.saripaar.annotation.AssertTrue;
import com.mobsandgeeks.saripaar.annotation.Checked;
import com.mobsandgeeks.saripaar.annotation.ConfirmEmail;
import com.mobsandgeeks.saripaar.annotation.ConfirmPassword;
import com.mobsandgeeks.saripaar.annotation.CreditCard;
import com.mobsandgeeks.saripaar.annotation.DecimalMax;
import com.mobsandgeeks.saripaar.annotation.DecimalMin;
import com.mobsandgeeks.saripaar.annotation.Digits;
import com.mobsandgeeks.saripaar.annotation.Domain;
import com.mobsandgeeks.saripaar.annotation.Email;
import com.mobsandgeeks.saripaar.annotation.Future;
import com.mobsandgeeks.saripaar.annotation.IpAddress;
import com.mobsandgeeks.saripaar.annotation.Isbn;
import com.mobsandgeeks.saripaar.annotation.Length;
import com.mobsandgeeks.saripaar.annotation.Max;
import com.mobsandgeeks.saripaar.annotation.Min;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;
import com.mobsandgeeks.saripaar.annotation.Optional;
import com.mobsandgeeks.saripaar.annotation.Order;
import com.mobsandgeeks.saripaar.annotation.Password;
import com.mobsandgeeks.saripaar.annotation.Past;
import com.mobsandgeeks.saripaar.annotation.Pattern;
import com.mobsandgeeks.saripaar.annotation.Select;
import com.mobsandgeeks.saripaar.annotation.Size;
import com.mobsandgeeks.saripaar.annotation.Url;
import com.mobsandgeeks.saripaar.annotation.ValidateUsing;
import com.mobsandgeeks.saripaar.exception.ConversionException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({ "unchecked", "ForLoopReplaceableByForEach" })
public class Validator {
    private static final Registry SARIPAAR_REGISTRY = new Registry();
    private static final Map<Class<? extends View>, HashMap<Class<?>, ViewDataAdapter>> REGISTERED_ADAPTERS = new HashMap();
    private Object mController;
    private Validator.Mode mValidationMode;
    private ValidationContext mValidationContext;
    private Map<View, ArrayList<Pair<Rule, ViewDataAdapter>>> mViewRulesMap;
    private boolean mOrderedFields;
    private SequenceComparator mSequenceComparator;
    private Validator.ViewValidatedAction mViewValidatedAction;
    private Handler mViewValidatedActionHandler;
    private Validator.ValidationListener mValidationListener;
    private Validator.AsyncValidationTask mAsyncValidationTask;
    private View mTargetView;

    public Validator(Object controller) {
        assertNotNull(controller, "controller");
        this.mController = controller;
        this.mValidationMode = Validator.Mode.BURST;
        this.mValidationContext = new ValidationContext();
        this.mSequenceComparator = new SequenceComparator();
        this.mViewValidatedAction = new DefaultViewValidatedAction();
    }

    public static void registerAnnotation(Class<? extends Annotation> ruleAnnotation) {
        SARIPAAR_REGISTRY.register(new Class[]{ruleAnnotation});
    }

    public static <VIEW extends View> void registerAnnotation(Class<? extends Annotation> annotation, Class<VIEW> viewType, ViewDataAdapter<VIEW, ?> viewDataAdapter) {
        ValidateUsing validateUsing = (ValidateUsing)annotation.getAnnotation(ValidateUsing.class);
        Class ruleDataType = Reflector.getRuleDataType(validateUsing);
        SARIPAAR_REGISTRY.register(viewType, ruleDataType, viewDataAdapter, new Class[]{annotation});
    }

    public static <VIEW extends View, DATA_TYPE> void registerAdapter(Class<VIEW> viewType, ViewDataAdapter<VIEW, DATA_TYPE> viewDataAdapter) {
        assertNotNull(viewType, "viewType");
        assertNotNull(viewDataAdapter, "viewDataAdapter");
        HashMap<Class<?>, ViewDataAdapter> dataTypeAdapterMap = (HashMap)REGISTERED_ADAPTERS.get(viewType);
        if (dataTypeAdapterMap == null) {
            dataTypeAdapterMap = new HashMap();
            REGISTERED_ADAPTERS.put(viewType, dataTypeAdapterMap);
        }

        Method getDataMethod = Reflector.findGetDataMethod(viewDataAdapter.getClass());
        Class<?> adapterDataType = getDataMethod.getReturnType();
        dataTypeAdapterMap.put(adapterDataType, viewDataAdapter);
    }

    public void setValidationListener(Validator.ValidationListener validationListener) {
        assertNotNull(validationListener, "validationListener");
        this.mValidationListener = validationListener;
    }

    public void setViewValidatedAction(Validator.ViewValidatedAction viewValidatedAction) {
        this.mViewValidatedAction = viewValidatedAction;
    }

    public void setValidationMode(Validator.Mode validationMode) {
        assertNotNull(validationMode, "validationMode");
        this.mValidationMode = validationMode;
    }

    public Validator.Mode getValidationMode() {
        return this.mValidationMode;
    }

    public void validate(boolean ultimate) {
        this.setValidationMode(ultimate ? Validator.Mode.BURST : Validator.Mode.IMMEDIATE);
        this.validate(false, ultimate);
    }

    public void validateOnly(View view, boolean ultimate) {
        this.mTargetView = view;
        this.validate(ultimate);
        this.mTargetView = null;
    }

    public void validateBefore(View view, boolean ultimate) {
        this.validateBefore(view, false, ultimate);
    }

    public void validateTill(View view, boolean ultimate) {
        this.validateTill(view, false, ultimate);
    }

    public void validate(boolean async, boolean ultimate) {
        this.createRulesSafelyAndLazily(false);
        View lastView = this.getLastView();
        if (Validator.Mode.BURST.equals(this.mValidationMode)) {
            this.validateUnorderedFieldsWithCallbackTill(lastView, async, ultimate);
        } else {
            if (!Validator.Mode.IMMEDIATE.equals(this.mValidationMode)) {
                throw new RuntimeException("This should never happen!");
            }

            String reasonSuffix = String.format("in %s mode.", Validator.Mode.IMMEDIATE.toString());
            this.validateOrderedFieldsWithCallbackTill(lastView, reasonSuffix, async, ultimate);
        }

    }

    public void validateBefore(View view, boolean async, boolean ultimate) {
        this.createRulesSafelyAndLazily(false);
        View previousView = this.getViewBefore(view);
        this.validateOrderedFieldsWithCallbackTill(previousView, "when using 'validateBefore(View)'.", async, ultimate);
    }

    public void validateTill(View view, boolean async, boolean ultimate) {
        this.validateOrderedFieldsWithCallbackTill(view, "when using 'validateTill(View)'.", async, ultimate);
    }

    public boolean isValidating() {
        return this.mAsyncValidationTask != null && this.mAsyncValidationTask.getStatus() != AsyncTask.Status.FINISHED;
    }

    public boolean cancelAsync() {
        boolean cancelled = false;
        if (this.mAsyncValidationTask != null) {
            cancelled = this.mAsyncValidationTask.cancel(true);
            this.mAsyncValidationTask = null;
        }

        return cancelled;
    }

    public <VIEW extends View> void put(VIEW view, QuickRule<VIEW>... quickRules) {
        assertNotNull(view, "view");
        assertNotNull(quickRules, "quickRules");
        if (quickRules.length == 0) {
            throw new IllegalArgumentException("'quickRules' cannot be empty.");
        } else {
            this.createRulesSafelyAndLazily(true);
            if (this.mOrderedFields && !this.mViewRulesMap.containsKey(view)) {
                String message = String.format("All fields are ordered, so this `%s` should be ordered too, declare the view as a field and add the `@Order` annotation.", view.getClass().getName());
                throw new IllegalStateException(message);
            } else {
                ArrayList<Pair<Rule, ViewDataAdapter>> ruleAdapterPairs = (ArrayList)this.mViewRulesMap.get(view);
                ruleAdapterPairs = ruleAdapterPairs == null ? new ArrayList() : ruleAdapterPairs;
                QuickRule[] var4 = quickRules;
                int var5 = quickRules.length;

                for(int var6 = 0; var6 < var5; ++var6) {
                    QuickRule quickRule = var4[var6];
                    if (quickRule != null) {
                        ruleAdapterPairs.add(new Pair(quickRule, (Object)null));
                    }
                }

                Collections.sort(ruleAdapterPairs, this.mSequenceComparator);
                this.mViewRulesMap.put(view, ruleAdapterPairs);
            }
        }
    }

    public void removeRules(View view) {
        assertNotNull(view, "view");
        if (this.mViewRulesMap == null) {
            this.createRulesSafelyAndLazily(false);
        }

        this.mViewRulesMap.remove(view);
    }

    static boolean isSaripaarAnnotation(Class<? extends Annotation> annotation) {
        return SARIPAAR_REGISTRY.getRegisteredAnnotations().contains(annotation);
    }

    private static void assertNotNull(Object object, String argumentName) {
        if (object == null) {
            String message = String.format("'%s' cannot be null.", argumentName);
            throw new IllegalArgumentException(message);
        }
    }

    private void createRulesSafelyAndLazily(boolean addingQuickRules) {
        if (this.mViewRulesMap == null) {
            List<Field> annotatedFields = this.getSaripaarAnnotatedFields(this.mController.getClass());
            this.mViewRulesMap = this.createRules(annotatedFields);
            this.mValidationContext.setViewRulesMap(this.mViewRulesMap);
        }

        if (!addingQuickRules && this.mViewRulesMap.size() == 0) {
            String message = "No rules found. You must have at least one rule to validate. If you are using custom annotations, make sure that you have registered them using the 'Validator.register()' method.";
            throw new IllegalStateException(message);
        }
    }

    private List<Field> getSaripaarAnnotatedFields(Class<?> controllerClass) {
        Set<Class<? extends Annotation>> saripaarAnnotations = SARIPAAR_REGISTRY.getRegisteredAnnotations();
        List<Field> annotatedFields = new ArrayList();
        List<Field> controllerViewFields = this.getControllerViewFields(controllerClass);
        Iterator var5 = controllerViewFields.iterator();

        while(var5.hasNext()) {
            Field field = (Field)var5.next();
            if (this.isSaripaarAnnotatedField(field, saripaarAnnotations)) {
                annotatedFields.add(field);
            }
        }

        SaripaarFieldsComparator comparator = new SaripaarFieldsComparator();
        Collections.sort(annotatedFields, comparator);
        this.mOrderedFields = annotatedFields.size() == 1 ? ((Field)annotatedFields.get(0)).getAnnotation(Order.class) != null : annotatedFields.size() != 0 && comparator.areOrderedFields();
        return annotatedFields;
    }

    private List<Field> getControllerViewFields(Class<?> controllerClass) {
        List<Field> controllerViewFields = new ArrayList();
        controllerViewFields.addAll(this.getViewFields(controllerClass));

        for(Class superClass = controllerClass.getSuperclass(); !superClass.equals(Object.class); superClass = superClass.getSuperclass()) {
            List<Field> viewFields = this.getViewFields(superClass);
            if (viewFields.size() > 0) {
                controllerViewFields.addAll(viewFields);
            }
        }

        return controllerViewFields;
    }

    private List<Field> getViewFields(Class<?> clazz) {
        List<Field> viewFields = new ArrayList();
        Field[] declaredFields = clazz.getDeclaredFields();
        Field[] var4 = declaredFields;
        int var5 = declaredFields.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            Field field = var4[var6];
            if (View.class.isAssignableFrom(field.getType())) {
                viewFields.add(field);
            }
        }

        return viewFields;
    }

    private boolean isSaripaarAnnotatedField(Field field, Set<Class<? extends Annotation>> registeredAnnotations) {
        boolean hasOrderAnnotation = field.getAnnotation(Order.class) != null;
        boolean hasSaripaarAnnotation = false;
        if (!hasOrderAnnotation) {
            Annotation[] annotations = field.getAnnotations();
            Annotation[] var6 = annotations;
            int var7 = annotations.length;

            for(int var8 = 0; var8 < var7; ++var8) {
                Annotation annotation = var6[var8];
                hasSaripaarAnnotation = registeredAnnotations.contains(annotation.annotationType());
                if (hasSaripaarAnnotation) {
                    break;
                }
            }
        }

        return hasOrderAnnotation || hasSaripaarAnnotation;
    }

    private Map<View, ArrayList<Pair<Rule, ViewDataAdapter>>> createRules(List<Field> annotatedFields) {
        Map<View, ArrayList<Pair<Rule, ViewDataAdapter>>> viewRulesMap = new LinkedHashMap();
        Iterator var3 = annotatedFields.iterator();

        while(var3.hasNext()) {
            Field field = (Field)var3.next();
            ArrayList<Pair<Rule, ViewDataAdapter>> ruleAdapterPairs = new ArrayList();
            Annotation[] fieldAnnotations = field.getAnnotations();
            Annotation[] var7 = fieldAnnotations;
            int var8 = fieldAnnotations.length;

            for(int var9 = 0; var9 < var8; ++var9) {
                Annotation fieldAnnotation = var7[var9];
                if (isSaripaarAnnotation(fieldAnnotation.annotationType())) {
                    Pair<Rule, ViewDataAdapter> ruleAdapterPair = this.getRuleAdapterPair(fieldAnnotation, field);
                    ruleAdapterPairs.add(ruleAdapterPair);
                }
            }

            Collections.sort(ruleAdapterPairs, this.mSequenceComparator);
            viewRulesMap.put(this.getView(field), ruleAdapterPairs);
        }

        return viewRulesMap;
    }

    private Pair<Rule, ViewDataAdapter> getRuleAdapterPair(Annotation saripaarAnnotation, Field viewField) {
        Class<? extends Annotation> annotationType = saripaarAnnotation.annotationType();
        Class<?> viewFieldType = viewField.getType();
        Class<?> ruleDataType = Reflector.getRuleDataType(saripaarAnnotation);
        ViewDataAdapter dataAdapter = this.getDataAdapter(annotationType, viewFieldType, ruleDataType);
        if (dataAdapter == null) {
            String viewType = viewFieldType.getName();
            String message = String.format("To use '%s' on '%s', register a '%s' that returns a '%s' from the '%s'.", annotationType.getName(), viewType, ViewDataAdapter.class.getName(), ruleDataType.getName(), viewType);
            throw new UnsupportedOperationException(message);
        } else {
            Class<? extends AnnotationRule> ruleType = this.getRuleType(saripaarAnnotation);
            AnnotationRule rule = Reflector.instantiateRule(ruleType, saripaarAnnotation, this.mValidationContext);
            return new Pair(rule, dataAdapter);
        }
    }

    private ViewDataAdapter getDataAdapter(Class<? extends Annotation> annotationType, Class<?> viewFieldType, Class<?> adapterDataType) {
        ViewDataAdapter dataAdapter = SARIPAAR_REGISTRY.getDataAdapter(annotationType, (Class)viewFieldType);
        if (dataAdapter == null) {
            HashMap<Class<?>, ViewDataAdapter> dataTypeAdapterMap = (HashMap)REGISTERED_ADAPTERS.get(viewFieldType);
            dataAdapter = dataTypeAdapterMap != null ? (ViewDataAdapter)dataTypeAdapterMap.get(adapterDataType) : null;
        }

        return dataAdapter;
    }

    private Class<? extends AnnotationRule> getRuleType(Annotation ruleAnnotation) {
        ValidateUsing validateUsing = (ValidateUsing)ruleAnnotation.annotationType().getAnnotation(ValidateUsing.class);
        return validateUsing != null ? validateUsing.value() : null;
    }

    private View getView(Field field) {
        View view = null;

        try {
            field.setAccessible(true);
            view = (View)field.get(this.mController);
        } catch (IllegalArgumentException var4) {
            var4.printStackTrace();
        } catch (IllegalAccessException var5) {
            var5.printStackTrace();
        }

        return view;
    }

    private void validateUnorderedFieldsWithCallbackTill(View view, boolean async, boolean ultimate) {
        this.validateFieldsWithCallbackTill(view, false, (String)null, async, ultimate);
    }

    private void validateOrderedFieldsWithCallbackTill(View view, String reasonSuffix, boolean async, boolean ultimate) {
        this.validateFieldsWithCallbackTill(view, true, reasonSuffix, async, ultimate);
    }

    private void validateFieldsWithCallbackTill(View view, boolean orderedFields, String reasonSuffix, boolean async, boolean ultimate) {
        this.createRulesSafelyAndLazily(false);
        if (async) {
            if (this.mAsyncValidationTask != null) {
                this.mAsyncValidationTask.cancel(true);
            }

            this.mAsyncValidationTask = new Validator.AsyncValidationTask(view, orderedFields, reasonSuffix, ultimate);
            this.mAsyncValidationTask.execute((Void[])null);
        } else {
            this.triggerValidationListenerCallback(this.validateTill(view, orderedFields, reasonSuffix, ultimate), ultimate);
        }

    }

    private synchronized Validator.ValidationReport validateTill(View view, boolean requiresOrderedRules, String reasonSuffix, boolean ultimate) {
        if (requiresOrderedRules && this.mTargetView == null) {
            this.assertOrderedFields(this.mOrderedFields, reasonSuffix);
        }

        assertNotNull(this.mValidationListener, "validationListener");
        return this.getValidationReport(view, this.mViewRulesMap, this.mValidationMode, ultimate);
    }

    private void triggerValidationListenerCallback(Validator.ValidationReport validationReport, boolean ultimate) {
        List<ValidationError> validationErrors = validationReport.errors;
        if (validationErrors.size() == 0 && !validationReport.hasMoreErrors) {
            if (this.mTargetView != null) {
                this.mValidationListener.onTargetedValidationSucceeded(this.mTargetView, ultimate);
            } else {
                this.mValidationListener.onValidationSucceeded(ultimate);
            }
        } else {
            this.mValidationListener.onValidationFailed(validationErrors, ultimate);
        }

    }

    private void assertOrderedFields(boolean orderedRules, String reasonSuffix) {
        if (!orderedRules) {
            String message = String.format("Rules are unordered, all view fields should be ordered using the '@Order' annotation " + reasonSuffix);
            throw new IllegalStateException(message);
        }
    }

    private Validator.ValidationReport getValidationReport(View targetView, Map<View, ArrayList<Pair<Rule, ViewDataAdapter>>> viewRulesMap, Validator.Mode validationMode, boolean ultimate) {
        List<ValidationError> validationErrors = new ArrayList();
        Set<View> views = viewRulesMap.keySet();
        boolean addErrorToReport = targetView != null;
        boolean hasMoreErrors = false;
        Iterator var9 = views.iterator();

        while(var9.hasNext()) {
            View view = (View)var9.next();
            ArrayList<Pair<Rule, ViewDataAdapter>> ruleAdapterPairs = (ArrayList)viewRulesMap.get(view);
            int nRules = ruleAdapterPairs.size();
            List<Rule> failedRules = null;

            for(int i = 0; i < nRules; ++i) {
                if (this.shouldValidate(view, ultimate)) {
                    Pair<Rule, ViewDataAdapter> ruleAdapterPair = (Pair)ruleAdapterPairs.get(i);
                    Rule failedRule = this.validateViewWithRule(view, (Rule)ruleAdapterPair.first, (ViewDataAdapter)ruleAdapterPair.second, ultimate);
                    boolean isLastRuleForView = nRules == i + 1;
                    if (failedRule != null) {
                        if (addErrorToReport) {
                            if (failedRules == null) {
                                failedRules = new ArrayList();
                                validationErrors.add(new ValidationError(view, failedRules));
                            }

                            failedRules.add(failedRule);
                        } else {
                            hasMoreErrors = true;
                        }

                        if (Validator.Mode.IMMEDIATE.equals(validationMode) && isLastRuleForView) {
                            return new Validator.ValidationReport(validationErrors, hasMoreErrors);
                        }
                    }

                    if (view.equals(targetView) && isLastRuleForView) {
                        addErrorToReport = false;
                    }
                }
            }

            boolean viewPassedAllRules = (failedRules == null || failedRules.size() == 0) && !hasMoreErrors;
            if (viewPassedAllRules && this.mViewValidatedAction != null) {
                this.triggerViewValidatedCallback(this.mViewValidatedAction, view);
            }
        }

        return new Validator.ValidationReport(validationErrors, hasMoreErrors);
    }

    private boolean shouldValidate(View view, boolean ultimate) {
        boolean focused = view instanceof ViewGroup ? ((ViewGroup)view).getFocusedChild() != null : view.isFocused();
        return view.isShown() && view.isEnabled() && (focused || ultimate || view == this.mTargetView);
    }

    private Rule validateViewWithRule(View view, Rule rule, ViewDataAdapter dataAdapter, boolean ultimate) {
        if (this.mTargetView != null && view != this.mTargetView) {
            return null;
        } else if (!ultimate && rule.isUltimate()) {
            return null;
        } else {
            boolean valid = false;
            if (rule instanceof AnnotationRule) {
                try {
                    Object data = dataAdapter.getData(view);
                    valid = rule.isValid(data);
                } catch (ConversionException var8) {
                    valid = false;
                    var8.printStackTrace();
                }
            } else if (rule instanceof QuickRule) {
                valid = rule.isValid(view);
            }

            return valid ? null : rule;
        }
    }

    private void triggerViewValidatedCallback(final Validator.ViewValidatedAction viewValidatedAction, final View view) {
        boolean isOnMainThread = Looper.myLooper() == Looper.getMainLooper();
        if (isOnMainThread) {
            viewValidatedAction.onAllRulesPassed(view);
        } else {
            this.runOnMainThread(new Runnable() {
                public void run() {
                    viewValidatedAction.onAllRulesPassed(view);
                }
            });
        }

    }

    private void runOnMainThread(Runnable runnable) {
        if (this.mViewValidatedActionHandler == null) {
            this.mViewValidatedActionHandler = new Handler(Looper.getMainLooper());
        }

        this.mViewValidatedActionHandler.post(runnable);
    }

    private View getLastView() {
        Set<View> views = this.mViewRulesMap.keySet();
        View lastView = null;

        View view;
        for(Iterator var3 = views.iterator(); var3.hasNext(); lastView = view) {
            view = (View)var3.next();
        }

        return lastView;
    }

    private View getViewBefore(View view) {
        ArrayList<View> views = new ArrayList(this.mViewRulesMap.keySet());
        int nViews = views.size();
        View previousView = null;

        for(int i = 0; i < nViews; ++i) {
            View currentView = (View)views.get(i);
            if (currentView == view) {
                previousView = i > 0 ? (View)views.get(i - 1) : null;
                break;
            }
        }

        return previousView;
    }

    static {
        SARIPAAR_REGISTRY.register(CheckBox.class, Boolean.class, new CheckBoxBooleanAdapter(), new Class[]{AssertFalse.class, AssertTrue.class, Checked.class});
        SARIPAAR_REGISTRY.register(RadioButton.class, Boolean.class, new RadioButtonBooleanAdapter(), new Class[]{AssertFalse.class, AssertTrue.class, Checked.class});
        SARIPAAR_REGISTRY.register(Spinner.class, Integer.class, new SpinnerIndexAdapter(), new Class[]{Select.class});
        SARIPAAR_REGISTRY.register(new Class[]{DecimalMax.class, DecimalMin.class});
        SARIPAAR_REGISTRY.register(new Class[]{Max.class, Min.class});
        SARIPAAR_REGISTRY.register(new Class[]{ConfirmEmail.class, ConfirmPassword.class, CreditCard.class, Domain.class, Email.class, IpAddress.class, Isbn.class, NotEmpty.class, Password.class, Pattern.class, Size.class, Url.class});
    }

    class AsyncValidationTask extends AsyncTask<Void, Void, Validator.ValidationReport> {
        private View mView;
        private boolean mOrderedRules;
        private String mReasonSuffix;
        private boolean mUltimate;

        public AsyncValidationTask(View view, boolean orderedRules, String reasonSuffix, boolean ultimate) {
            this.mView = view;
            this.mOrderedRules = orderedRules;
            this.mReasonSuffix = reasonSuffix;
            this.mUltimate = ultimate;
        }

        protected Validator.ValidationReport doInBackground(Void... params) {
            return Validator.this.validateTill(this.mView, this.mOrderedRules, this.mReasonSuffix, this.mUltimate);
        }

        protected void onPostExecute(Validator.ValidationReport validationReport) {
            Validator.this.triggerValidationListenerCallback(validationReport, this.mUltimate);
        }
    }

    static class ValidationReport {
        List<ValidationError> errors;
        boolean hasMoreErrors;

        ValidationReport(List<ValidationError> errors, boolean hasMoreErrors) {
            this.errors = errors;
            this.hasMoreErrors = hasMoreErrors;
        }
    }

    public static enum Mode {
        BURST,
        IMMEDIATE;

        private Mode() {
        }
    }

    public interface ViewValidatedAction {
        void onAllRulesPassed(View var1);
    }

    public interface ValidationListener {
        void onValidationSucceeded(boolean var1);

        void onTargetedValidationSucceeded(View var1, boolean var2);

        void onValidationFailed(List<ValidationError> var1, boolean var2);
    }
}
