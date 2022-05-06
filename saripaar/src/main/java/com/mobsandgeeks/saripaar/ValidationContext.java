/*
 * Copyright (C) 2015 Mobs & Geeks
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

import android.content.Context;
import android.util.Pair;
import android.view.View;

import com.mobsandgeeks.saripaar.adapter.ViewDataAdapter;
import com.mobsandgeeks.saripaar.annotation.ValidateUsing;
import com.mobsandgeeks.saripaar.exception.ConversionException;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grants access to information about other {@link android.view.View}s in the controller object.
 *
 * @author Ragunath Jawahar {@literal <rj@mobsandgeeks.com>}
 * @since 2.0
 */
public class ValidationContext {
    Map<View, ArrayList<Pair<Rule, ViewDataAdapter>>> mViewRulesMap;

    ValidationContext() {
    }

    public List<View> getAnnotatedViews(Class<? extends Annotation> saripaarAnnotation) {
        this.assertNotNull(saripaarAnnotation, "saripaarAnnotation");
        this.assertIsRegisteredAnnotation(saripaarAnnotation);
        Class<? extends AnnotationRule> annotationRuleClass = this.getRuleClass(saripaarAnnotation);
        List<View> annotatedViews = new ArrayList();
        Set<View> views = this.mViewRulesMap.keySet();
        Iterator var5 = views.iterator();

        while(var5.hasNext()) {
            View view = (View)var5.next();
            ArrayList<Pair<Rule, ViewDataAdapter>> ruleAdapterPairs = (ArrayList)this.mViewRulesMap.get(view);
            Iterator var8 = ruleAdapterPairs.iterator();

            while(var8.hasNext()) {
                Pair<Rule, ViewDataAdapter> ruleAdapterPair = (Pair)var8.next();
                boolean uniqueMatchingView = annotationRuleClass.equals(((Rule)ruleAdapterPair.first).getClass()) && !annotatedViews.contains(view);
                if (uniqueMatchingView) {
                    annotatedViews.add(view);
                }
            }
        }

        return annotatedViews;
    }

    public Object getData(View view, Class<? extends Annotation> saripaarAnnotation) {
        this.assertNotNull(view, "view");
        this.assertNotNull(saripaarAnnotation, "saripaarAnnotation");
        Object data = null;
        ArrayList<Pair<Rule, ViewDataAdapter>> ruleAdapterPairs = (ArrayList)this.mViewRulesMap.get(view);
        Class<? extends AnnotationRule> annotationRuleClass = this.getRuleClass(saripaarAnnotation);
        Iterator var6 = ruleAdapterPairs.iterator();

        while(var6.hasNext()) {
            Pair<Rule, ViewDataAdapter> ruleAdapterPair = (Pair)var6.next();
            if (annotationRuleClass.equals(((Rule)ruleAdapterPair.first).getClass())) {
                try {
                    data = ((ViewDataAdapter)ruleAdapterPair.second).getData(view);
                } catch (ConversionException var9) {
                    var9.printStackTrace();
                }
            }
        }

        return data;
    }

    void setViewRulesMap(Map<View, ArrayList<Pair<Rule, ViewDataAdapter>>> viewRulesMap) {
        this.mViewRulesMap = viewRulesMap;
    }

    private void assertNotNull(Object object, String argumentName) {
        if (object == null) {
            String message = String.format("'%s' cannot be null.", argumentName);
            throw new IllegalArgumentException(message);
        }
    }

    private void assertIsRegisteredAnnotation(Class<? extends Annotation> saripaarAnnotation) {
        if (!Validator.isSaripaarAnnotation(saripaarAnnotation)) {
            String message = String.format("%s is not a registered Saripaar annotation.", saripaarAnnotation.getName());
            throw new IllegalArgumentException(message);
        }
    }

    private Class<? extends AnnotationRule> getRuleClass(Class<? extends Annotation> saripaarAnnotation) {
        ValidateUsing validateUsingAnnotation = (ValidateUsing)saripaarAnnotation.getAnnotation(ValidateUsing.class);
        return validateUsingAnnotation.value();
    }
}
