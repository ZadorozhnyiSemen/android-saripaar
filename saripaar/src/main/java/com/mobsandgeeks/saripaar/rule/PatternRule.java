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

package com.mobsandgeeks.saripaar.rule;

import com.mobsandgeeks.saripaar.AnnotationRule;
import com.mobsandgeeks.saripaar.ContextualAnnotationRule;
import com.mobsandgeeks.saripaar.ValidationContext;
import com.mobsandgeeks.saripaar.annotation.Pattern;

import commons.validator.routines.RegexValidator;

/**
 * @author Ragunath Jawahar {@literal <rj@mobsandgeeks.com>}
 * @since 2.0
 */
public class PatternRule extends AnnotationRule<Pattern, String> {
    protected PatternRule(Pattern pattern) {
        super(pattern);
    }

    public boolean isValid(String text) {
        String regex = ((Pattern)this.mRuleAnnotation).regex();
        boolean caseSensitive = ((Pattern)this.mRuleAnnotation).caseSensitive();
        return (new RegexValidator(regex, caseSensitive)).isValid(text);
    }
}
