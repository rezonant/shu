package rezonant.shu.threads;

/**
 * Created by liam on 8/3/14.
 */
public class MessageThreadRequest extends ThreadRequest {
    public MessageThreadRequest(Thread thread, String message) {
        super(thread);

        this.message = message;
    }

    private String message;

    public String getMessage() {
        return message;
    }
}
