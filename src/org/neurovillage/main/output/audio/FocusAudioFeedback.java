package org.neurovillage.main.output.audio;

import java.io.File;
import java.io.IOException;

import org.neurovillage.main.output.feedback.Feedback;
import org.neurovillage.model.FeedbackSettings;
import org.neurovillage.tools.ResourceManager;

import com.jsyn.JSyn;
import com.jsyn.Synthesizer;
import com.jsyn.data.FloatSample;
import com.jsyn.devices.AudioDeviceFactory;
import com.jsyn.ports.QueueDataCommand;
import com.jsyn.unitgen.LineOut;
import com.jsyn.unitgen.VariableRateDataReader;
import com.jsyn.unitgen.VariableRateMonoReader;
import com.jsyn.unitgen.VariableRateStereoReader;
import com.jsyn.util.SampleLoader;

public class FocusAudioFeedback extends Feedback
{
	private Synthesizer synth;
	private JSynThread jsynThread;
	private LineOut lineOut;
	private FloatSample sample;
	private VariableRateDataReader samplePlayer;
	private double rateX;

	public FocusAudioFeedback(FeedbackSettings feedbackSettings)
	{ 
		super(feedbackSettings);
//		JSynThread jsynThread = new JSynThread();
//		jsynThread.run();
	}

	@Override
	public void run()
	{
		this.synth = JSyn.createSynthesizer(AudioDeviceFactory.createAudioDeviceManager(true));
//		this.synth = jsynThread.getSynth();

		this.synth.add(lineOut = new LineOut());
		try
		{
			
			this.sample = SampleLoader.loadFloatSample(ResourceManager.getInstance().getResource("audio/pad_.wav"));
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		if (sample.getChannelsPerFrame() == 1)
		{
			synth.add(samplePlayer = new VariableRateMonoReader());
			samplePlayer.output.connect(0, lineOut.input, 0);
		} else if (sample.getChannelsPerFrame() == 2)
		{
			synth.add(samplePlayer = new VariableRateStereoReader());
			samplePlayer.output.connect(0, lineOut.input, 0);
			samplePlayer.output.connect(1, lineOut.input, 1);
		} else
		{
			throw new RuntimeException("Can only play mono or stereo samples.");
		}

		int loopStartFrame = (int) (sample.getNumFrames() * 0.2);
		int loopSize = (int) (sample.getNumFrames() * 0.7);

		samplePlayer.rate.set(rateX = sample.getFrameRate());

		this.synth.start();
		lineOut.start();

		samplePlayer.amplitude.set(0);

		samplePlayer.dataQueue.queue(sample, 0, loopStartFrame);

		if ((loopStartFrame + loopSize) > sample.getNumFrames())
		{
			loopSize = sample.getNumFrames() - loopStartFrame;
		}
		int crossFadeSize = (int) (2000);

		QueueDataCommand command = samplePlayer.dataQueue.createQueueDataCommand(sample, loopStartFrame, loopSize);
		command.setNumLoops(-1);
		command.setSkipIfOthers(true);
		command.setCrossFadeIn(crossFadeSize);

		System.out.println("Queue: " + loopStartFrame + ", #" + loopSize + ", X=" + crossFadeSize);
		synth.queueCommand(command);
		
		running = true;

	}

	@Override
	public void updateCurrentFeedback(double currentFeedback)
	{
		super.updateCurrentFeedback(currentFeedback);
		samplePlayer.amplitude.set(Math.max(currentFeedback,0));
//		samplePlayer.amplitude.set(Math.max(Math.min(1d, currentFeedback), 0d));
	}

}
