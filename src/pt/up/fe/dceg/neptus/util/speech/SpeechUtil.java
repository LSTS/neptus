/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2007/06/03
 * $Id:: SpeechUtil.java 9615 2012-12-30 23:08:28Z pdias                        $:
 */
package pt.up.fe.dceg.neptus.util.speech;

import java.util.LinkedList;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.speech.EngineCreate;
import javax.speech.EngineList;
import javax.speech.EngineStateError;
import javax.speech.synthesis.Synthesizer;
import javax.speech.synthesis.SynthesizerModeDesc;
import javax.speech.synthesis.Voice;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;
import pt.up.fe.dceg.neptus.util.conf.GeneralPreferences;
import pt.up.fe.dceg.neptus.util.conf.PreferencesListener;

/**
 * @author Paulo Dias
 *
 */
public class SpeechUtil {
	/**
	 * Singleton
	 */
	private static SpeechUtil speechUtil = null;

	public static boolean speechOn = false;
	
	private static LinkedList<String> textQueue = new LinkedList<String>();
	private ReadProcessor readProcessor = new ReadProcessor();
	private Synthesizer synth;
	private static final SpeechUtilControl speechUtilControl = new SpeechUtilControl();
	
	static {
	    speechOn = GeneralPreferences.speechOn;
		GeneralPreferences.addPreferencesListener(speechUtilControl);
	}
	
	/*
	 * kevin in an 8kHz general domain diphone voice
	 */
	Voice kevin = new Voice("kevin", Voice.GENDER_DONT_CARE,
			Voice.AGE_DONT_CARE, null);

	/*
	 * kevin16 in a 16kHz general domain diphone voice
	 */
	Voice kevinHQ = new Voice("kevin16", Voice.GENDER_DONT_CARE,
			Voice.AGE_DONT_CARE, null);

	
	private SpeechUtil() {
		try {
			SynthesizerModeDesc desc =
				new SynthesizerModeDesc(null,
						"general", /* use "time" or "general" */
						Locale.US,
						Boolean.FALSE,
						null);

			com.sun.speech.freetts.jsapi.FreeTTSEngineCentral central = new com.sun.speech.freetts.jsapi.FreeTTSEngineCentral();
			EngineList list = central.createEngineList(desc);

			if (list.size() > 0) {
				EngineCreate creator = (EngineCreate) list.get(0);
				synth = (Synthesizer) creator.createEngine();
			}
			if (synth == null) {
				System.err.println("Cannot create synthesizer");
				speechUtil = null;
				return;
			}
			synth.allocate();
			synth.resume();
		    synth.getSynthesizerProperties().setVoice(kevinHQ);
		    
		    readProcessor.start();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		catch (Error e) {
			e.printStackTrace();
		}
	}
	
	public static void removeStringsFromQueue(String regexp) {
	    Pattern p = Pattern.compile(regexp);
        
	    synchronized (textQueue) {
	        for (int i = 0; i < textQueue.size(); i++) {
	            Matcher m = p.matcher(textQueue.get(i));
	            if (m.matches()) {
	                NeptusLog.pub().warn("removed "+textQueue.get(i)+" from speech syhthesis queue");
	                textQueue.remove(i);	                
	                i--;
	            }
	        }
        }
	    
	    if (speechUtil != null ) {
	        String phrase = speechUtil.readProcessor.getCurrentPhrase();
	        if (phrase != null) {
    	        Matcher m = p.matcher(speechUtil.readProcessor.getCurrentPhrase());
    	        if (m.matches()) {
    	            stop();
    	            start();
    	        }
	        }
	    }
	    
	}

	public static boolean readSimpleText(String text) {
		start();
		if (speechUtil == null)
			return false;
		synchronized (textQueue) {
		    if (!textQueue.contains(text))
		        textQueue.add(text);
		}
		synchronized (speechUtil.readProcessor) {
			speechUtil.readProcessor.notify();			
		}
		return true;
	}

	public synchronized static void start() {
		if (!SpeechUtil.speechOn)
			return;
		if (speechUtil == null)
			speechUtil = new SpeechUtil();
	}
	
	public synchronized static void stop() {
		if (speechUtil == null)
			return;
	    // clean up
	    try {
			speechUtil.synth.deallocate();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		speechUtil.readProcessor.stopProcessing();
		speechUtil = null;
		textQueue.clear();
	}
	
	private synchronized void readText(String text) {
        // Speak the context of the text
        // field, ignoring JSML tags. 
        // Pass null as the second
        // argument because I am not
        // interested in attaching a
        // listener that receives events
        // as text is spoken.

        try {
			synth.speakPlainText (text, null);
		} catch (EngineStateError e1) {
		}

        try {
            synth.waitEngineState (Synthesizer.QUEUE_EMPTY);
        }
        catch (InterruptedException e) {
        }
	}
	
	private final class ReadProcessor extends Thread {
		private boolean running = true;
        protected String text;
		public ReadProcessor() {
			super("MessageProcessor");
		}

        public void stopProcessing() {
			running = false;
			synchronized (this) {
				this.notify();
			}
		}


		/**
         * @return the text
         */
        public String getCurrentPhrase() {
            return text;
        }

        @Override
		public void run() {
			while (running) {
				try {

					// System.out.println("::::::::::");
					synchronized (textQueue) {
						text = textQueue.remove();
					}
					try {
					    readText(text);
					}
					catch (Exception e) {
						NeptusLog.pub().error(this+" error on child processing method",e);
					}
				}
				catch (NoSuchElementException e1) {
					synchronized (this) {
						try {
							// System.out.println("zzzzzzzzzz");
							wait();
							// System.out.println("wakeeeeeee");
							// System.out.flush();
						}
						catch (Exception e) {
							NeptusLog.pub().warn(this, e);
							// e.printStackTrace();
						}
					}
				}
			}
		}
	}

	
	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		//SpeechUtil.start();
	    SpeechUtil.speechOn = true;
	    
	    ConfigFetch.initialize();
	    String[] vehicles = VehiclesHolder.getVehiclesArray(); 
	    for (int i = vehicles.length-1; i>=0; i--) {
	        System.out.println(vehicles[i]);
	        SpeechUtil.readSimpleText(vehicles[i]+"is ready");
	    }	   
	}

}

class SpeechUtilControl implements PreferencesListener {

	public void preferencesUpdated() {
	    boolean son = GeneralPreferences.speechOn;
	    SpeechUtil.speechOn = son;
	    if (!son)
	        SpeechUtil.stop();
	}	
}