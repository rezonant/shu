package rezonant.shu.threads;

/**
* Created by liam on 7/5/14.
*/
public class YesNoThreadRequest extends ThreadRequest {
	public YesNoThreadRequest(Thread thread, String prompt)
	{
		super(thread);
		this.prompt = prompt;
	}

	private String prompt;

	public String getPrompt() {
		return prompt;
	}
}
