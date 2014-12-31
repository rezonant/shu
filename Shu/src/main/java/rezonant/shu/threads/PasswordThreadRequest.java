package rezonant.shu.threads;

/**
 * Created by liam on 8/3/14.
 */
public class PasswordThreadRequest extends ThreadRequest {
    public PasswordThreadRequest(Thread thread, String message) {
        super(thread);

        this.message = message;
    }

    private String message;

    public String getMessage() {
        return message;
    }
}
