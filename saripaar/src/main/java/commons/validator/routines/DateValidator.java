package commons.validator.routines;

import com.mobsandgeeks.saripaar.TimePrecision;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DateValidator {
    private DateValidator.Condition mCondition;
    public static final DateValidator FUTURE_VALIDATOR;
    public static final DateValidator PAST_VALIDATOR;

    private DateValidator(DateValidator.Condition condition) {
        this.mCondition = condition;
    }

    public boolean isValid(boolean strict, TimePrecision precision, int offset, Date date) {
        Calendar currentCalendar = new GregorianCalendar();
        Calendar passedCalendar = new GregorianCalendar();
        passedCalendar.setTime(date);
        GregorianCalendar destCalendar;
        GregorianCalendar srcCalendar;
        switch(precision) {
            case DAY:
                destCalendar = new GregorianCalendar(currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH), currentCalendar.get(Calendar.DAY_OF_MONTH));
                srcCalendar = new GregorianCalendar(passedCalendar.get(Calendar.YEAR), passedCalendar.get(Calendar.MONTH), passedCalendar.get(Calendar.DAY_OF_MONTH));
                destCalendar.add(Calendar.DAY_OF_MONTH, offset);
                break;
            case MONTH:
                destCalendar = new GregorianCalendar(currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH), 1);
                srcCalendar = new GregorianCalendar(passedCalendar.get(Calendar.YEAR), passedCalendar.get(Calendar.MONTH), 1);
                destCalendar.add(Calendar.MONTH, offset);
                break;
            case YEAR:
                destCalendar = new GregorianCalendar(currentCalendar.get(Calendar.YEAR), 1, 1);
                srcCalendar = new GregorianCalendar(passedCalendar.get(Calendar.YEAR), 1, 1);
                destCalendar.add(Calendar.YEAR, offset);
                break;
            default:
                throw new IllegalArgumentException("Unknown precision:" + precision);
        }

        long diff = srcCalendar.getTimeInMillis() - destCalendar.getTimeInMillis();
        if (this.mCondition == DateValidator.Condition.PAST) {
            diff = -diff;
        }

        return diff > 0L;
    }

    static {
        FUTURE_VALIDATOR = new DateValidator(DateValidator.Condition.FUTURE);
        PAST_VALIDATOR = new DateValidator(DateValidator.Condition.PAST);
    }

    private static enum Condition {
        FUTURE,
        PAST;

        private Condition() {
        }
    }
}