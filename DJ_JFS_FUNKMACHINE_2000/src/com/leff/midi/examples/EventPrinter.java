package com.leff.midi.examples;

import java.io.File;
import java.io.IOException;

import com.leff.midi.MidiFile;
import com.leff.midi.event.MidiEvent;
import com.leff.midi.event.NoteOn;
import com.leff.midi.event.meta.Tempo;
import com.leff.midi.util.MidiEventListener;
import com.leff.midi.util.MidiProcessor;

public class EventPrinter implements MidiEventListener {

	private String mLabel;
	public EventPrinter(String label) {
		mLabel = label;
	}

	// 0. Implement the listener functions that will be called by the MidiProcessor
	@Override
	public void onStart(boolean fromBeginning) {
		
		// Note that you will receive this once for each event you've registered for
		if(fromBeginning) {
			System.out.println(mLabel + " Begin!");
		}
	}

	@Override
	public void onEvent(MidiEvent event, long ms) {
		System.out.println(mLabel + " received event: " + event);
	}

	@Override
	public void onStop(boolean finished) {
		
		// Note that you will receive this once for each event you've registered for
		if(finished) {
			System.out.println(mLabel + " Finished!");
		}
	}
	
	public static void main(String[] args) {
		
		// 1. Read in a MidiFile
		MidiFile midi = null;
		try {
			midi = new MidiFile(new File("inputfile.mid"));
		} catch(IOException e) {
			System.err.println(e);
			return;
		}
		
		// 2. Create a MidiProcessor
		MidiProcessor processor = new MidiProcessor(midi);
		
		// 3. Register listeners for the events you're interested in
		EventPrinter ep = new EventPrinter("Individual Listener");
		processor.registerEventListener(ep, Tempo.class);
		processor.registerEventListener(ep, NoteOn.class);
		
		// or listen for all events:
		EventPrinter ep2 = new EventPrinter("Listener For All");
		processor.registerListenerForAllEvents(ep2);
		
		// 4. Start the processor
		processor.start();
		
		// Listeners will be triggered in real time with the MIDI events
	}
}
