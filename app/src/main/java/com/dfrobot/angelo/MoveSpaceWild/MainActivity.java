package com.dfrobot.angelo.MoveSpaceWild;

import android.os.Bundle;
import android.content.Intent;
import android.os.Environment;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

public class MainActivity  extends BlunoLibrary {
	private Button buttonScan;
    private Button launchExperiment;
    private Button confirmParticipant;
    private TextView bluetoothDebug;
    public TextView expeDebug;

	private String receivedString;
	private String participant;

	private PrintWriter pw_stimuli = null;

    private boolean experimentOn = false;
    private boolean connectionOK = false;
    private boolean filesOK = false;
    private boolean trialOn = false;

    private Stimulus soundStimulus;

    private ArrayList<String> trials = new ArrayList<String>();

    private long timestamp;
    private long initialTimeStamp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

        this.soundStimulus = new Stimulus(this);
        this.soundStimulus.initSounds();

        this.bluetoothDebug = (TextView) findViewById(R.id.bluetoothDebug);
        this.expeDebug = (TextView) findViewById(R.id.expeDebug);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        onCreateProcess();														//onCreate Process by BlunoLibrary


        serialBegin(115200);													//set the Uart Baudrate on BLE chip to 115200

	// Send text to arduino
		// 			serialSend(serialSendText.getText().toString());				//send the data to the BLUNO

        buttonScan = (Button) findViewById(R.id.buttonScan);					//initial the button for scanning the BLE device
        this.launchExperiment = (Button) findViewById(R.id.launchExperiment);
        this.launchExperiment.setVisibility(View.INVISIBLE);

        this.confirmParticipant = (Button) findViewById(R.id.confirmParticipant);



	}

	protected void onResume(){
		super.onResume();
		System.out.println("BlUNOActivity onResume");
		onResumeProcess();														//onResume Process by BlunoLibrary
	}
	
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		onActivityResultProcess(requestCode, resultCode, data);					//onActivityResult Process by BlunoLibrary
		super.onActivityResult(requestCode, resultCode, data);
	}
	
    @Override
    protected void onPause() {
        super.onPause();
        onPauseProcess();														//onPause Process by BlunoLibrary
    }
	
	protected void onStop() {
		super.onStop();
		onStopProcess();														//onStop Process by BlunoLibrary
	}
    
	@Override
    protected void onDestroy() {
        super.onDestroy();

        onDestroyProcess();														//onDestroy Process by BlunoLibrary
    }

	@Override
	public void onConectionStateChange(connectionStateEnum theConnectionState) {//Once connection state changes, this function will be called
		switch (theConnectionState) {											//Four connection state
		case isConnected:
			buttonScan.setText("Connected");
            this.updateLaunchExperiment(true, this.filesOK);
			break;
		case isConnecting:
			buttonScan.setText("Connecting");
            this.updateLaunchExperiment(false, this.filesOK);
			break;
		case isToScan:
			buttonScan.setText("Scan");
            this.updateLaunchExperiment(false, this.filesOK);
			break;
		case isScanning:
			buttonScan.setText("Scanning");
            this.updateLaunchExperiment(false, this.filesOK);
			break;
		case isDisconnecting:
			buttonScan.setText("isDisconnecting");
            this.updateLaunchExperiment(false, this.filesOK);
			break;
		default:
			break;
		}
	}

	@Override
	public void onSerialReceived(String theString) {                            //Once connection data received, this function will be called
        receivedString = theString;

        this.runOnUiThread(new Runnable()
        {
            public void run()
            {
                bluetoothDebug.setText(receivedString);
            }
        });

        if (this.experimentOn && this.trialOn) {
            //this.setTrials(false);
            this.logArduino(theString);
        }
    }


	public void buttonScanClick(View view)
	{
		buttonScanOnClickProcess();
	}

	public void createLogFiles(View view)
	{
        EditText participant_id = (EditText) findViewById(R.id.participantID);


        this.participant = participant_id.getText().toString();
        if (!this.participant.isEmpty()) {
            String path = Environment.getExternalStorageDirectory().getPath();

            File file = new File(path, "P_" + this.participant
                    + "_ecological_stimuli.csv");

            if (file.exists()) {
                file.delete();
                System.out.println("Deleted stimulus file");
            }


            try {
                this.pw_stimuli = new PrintWriter(new FileOutputStream(file));

                this.pw_stimuli.println("Timestamp,CompletionTime,Block,Trial,Stimulus,Recognized,Correct");
                this.pw_stimuli.flush();
                System.out.println("Created new stimulus file");
                this.updateLaunchExperiment(this.connectionOK, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
	}

    public void updateLaunchExperiment(boolean b1, boolean b2)
    {
        this.connectionOK = b1;
        this.filesOK = b2;
        if (this.filesOK && this.connectionOK)
        {
            this.launchExperiment.setVisibility(View.VISIBLE);
            //this.falsePositives.setVisibility(View.VISIBLE);
        }
        else
        {
            this.launchExperiment.setVisibility(View.INVISIBLE);
            //this.falsePositives.setVisibility(View.INVISIBLE);
        }

    }

    public void launchExperiment(View view)
    {
        System.out.println("Let's launch it!");
        this.initialTimeStamp = System.currentTimeMillis();
        this.disableButtons();
        this.launchExperiment.setText("Block in Progress!");
        this.experimentOn = true;
        this.timestamp = System.currentTimeMillis();

        this.soundStimulus.start();

        /*try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.soundStimulus.playSound(0);*/
    }

    /*
    public void falsePositives(View view)
    {
        System.out.println("Let's launch it!");
        //this.falsePositives.setText("False Pos. in Progress!");
        this.disableButtons();
        this.experimentOn = true;
        this.timestamp = System.currentTimeMillis();
        this.soundStimulus.setFalsePositives();
        this.soundStimulus.start();
    }*/

    public void logArduino (String theString) {
        theString = theString.replaceAll("(\\r|\\n)", "");
        int selected = Integer.parseInt(theString);
        this.soundStimulus.unlockThread(selected);
        long now = System.currentTimeMillis();
        double tstp = (now - this.timestamp) / 1000d;
        int current = this.soundStimulus.getCurrentStimulus();
        double sinceBegin = (now - this.initialTimeStamp) / 1000d;
        String trial = sinceBegin + ","
                + tstp + ","
                + this.soundStimulus.getBlockNumber() + ","
                + this.soundStimulus.getTrialNumber() + ","
                + current + ","
                + selected + ","
                + (current == selected);
        this.trials.add(trial);
        this.pw_stimuli.flush();

        // TODO UNLOCK THE DAMN THREAD
        this.soundStimulus.unlockThread(selected);


    }

    public void flushLogs()
    {
        for (String s: this.trials)
        {
            this.pw_stimuli.println(s);
        }
        this.pw_stimuli.flush();
        this.trials = new ArrayList<>();
    }

    public void panicButton(View view)
    {

        this.endExperiment();
    }

    public void endExperiment()
    {
        for (String s: this.trials)
        {
            this.pw_stimuli.println(s);
        }
        this.pw_stimuli.println("End of experiment!");
        this.pw_stimuli.flush();
        this.pw_stimuli.close();
        System.exit(0);
    }

    public void setTrials(boolean b) {
        System.out.println("trialOn=" + b);
        this.trialOn = b;
    }

    public void newTimeStamp()
    {
        this.timestamp = System.currentTimeMillis();
    }

    public void disableButtons()
    {
        this.launchExperiment.setEnabled(false);
        //this.falsePositives.setEnabled(false);
        this.buttonScan.setEnabled(false);
        this.confirmParticipant.setEnabled(false);
    }


    public long getInitialTimeStamp()
    {
        return this.initialTimeStamp;
    }

}