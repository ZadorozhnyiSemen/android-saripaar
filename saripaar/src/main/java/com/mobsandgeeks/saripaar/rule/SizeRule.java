package com.mobsandgeeks.saripaar.rule;

import android.widget.EditText;

import com.mobsandgeeks.saripaar.AnnotationRule;
import com.mobsandgeeks.saripaar.QuickRule;
import com.mobsandgeeks.saripaar.annotation.Size;

public class SizeRule extends AnnotationRule<Size, String> {
    protected SizeRule(Size size) {
        super(size);
    }

    public boolean isValid(String text) {
        if (text == null) {
            throw new IllegalArgumentException("'text' cannot be null.");
        } else {
            int ruleMin = ((Size)this.mRuleAnnotation).min();
            int ruleMax = ((Size)this.mRuleAnnotation).max();
            String filter = ((Size)this.mRuleAnnotation).filter();
            this.assertMinMax(ruleMin, ruleMax);
            int length;
            if (filter.length() != 0) {
                String regex = "[^" + filter + "]";
                length = ((Size)this.mRuleAnnotation).trim() ? text.trim().replaceAll(regex, "").length() : text.replaceAll(regex, "").length();
            } else {
                length = ((Size)this.mRuleAnnotation).trim() ? text.trim().length() : text.length();
            }

            boolean minIsValid = true;
            if (ruleMin != -2147483648) {
                minIsValid = length >= ruleMin;
            }

            boolean maxIsValid = true;
            if (ruleMax != 2147483647) {
                maxIsValid = length <= ruleMax;
            }

            return minIsValid && maxIsValid;
        }
    }

    private void assertMinMax(int min, int max) {
        if (min > max) {
            String message = String.format("'min' (%d) should be less than or equal to 'max' (%d).", min, max);
            throw new IllegalStateException(message);
        }
    }

    private static class Ruler extends QuickRule<EditText> {
        private Ruler() {
        }

        public boolean isValid(EditText view) {
            return false;
        }

        public int getErrorCode() {
            return 0;
        }
    }
}
