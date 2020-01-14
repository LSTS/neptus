/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Paulo Dias
 * 2007/06/03
 */
package pt.lsts.neptus.util.speech;

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

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import pt.lsts.neptus.util.conf.PreferencesListener;

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

					// NeptusLog.pub().info("<###>::::::::::");
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
							// NeptusLog.pub().info("<###>zzzzzzzzzz");
							wait();
							// NeptusLog.pub().info("<###>wakeeeeeee");
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
	        NeptusLog.pub().info("<###> "+vehicles[i]);
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