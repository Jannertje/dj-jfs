package com.jfs.funkmachine2000;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeSet;

import com.leff.midi.MidiFile;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.MidiEvent;
import com.leff.midi.event.ProgramChange;
import com.leff.midi.event.meta.Tempo;
import com.leff.midi.event.meta.TimeSignature;

public class StringToMidi {
	int nrows, ncols;
	private int rootNote;
	boolean hasModifier = true;
	String[] modifier;
	ArrayList<MidiTrack> tracks;

	StringToMidi(String gridString) {
		this(gridString, 120, 60);
	}
	
	StringToMidi(String gridString, int bpm) {
		this(gridString, bpm, 60);
	}

	StringToMidi(String gridString, int bpm, int rootNote) {
		this.rootNote = rootNote; // 60 is middle C
		tracks = initMidi(bpm);
		String gridStrSplit[] = gridString.split(":");

		System.out.print("Input String: " + gridString);
		readDimensions(gridStrSplit[0]);
		String alphabet = readAlphabet(gridStrSplit[1]);
		readGrid(gridStrSplit[2], alphabet);
	}

	private static ArrayList<MidiTrack> initMidi(int bpm) {
		ArrayList<MidiTrack> tracks = new ArrayList<MidiTrack>();
		tracks.add(new MidiTrack()); // tempoTrack
		TimeSignature ts = new TimeSignature();
		ts.setTimeSignature(4, 4, TimeSignature.DEFAULT_METER,
				TimeSignature.DEFAULT_DIVISION);
		Tempo t = new Tempo();
		t.setBpm(bpm);
		tracks.get(0).insertEvent(ts);
		tracks.get(0).insertEvent(t);
		return tracks;
	}

	private void readDimensions(String dimStr) {
		String helper = "";
		// read dimensions of grid
		for (int i = 0; i < dimStr.length(); i++) {
			if (Character.isDigit(dimStr.charAt(i)))
				helper = helper + dimStr.charAt(i);
			else if (dimStr.charAt(i) == ',') {
				nrows = Integer.parseInt(helper);
				helper = "";
			}
		}
		ncols = Integer.parseInt(helper);
		System.out.print("\nRows: " + nrows + "\nColumns: " + ncols);
		if (nrows < (7 + (hasModifier ? 1 : 0)))
			System.out
					.print("\nWarning: number of rows specified does not span a whole octave.");
		if (ncols % 4 != 0)
			System.out
					.print("\nWarning: number of columns not divisible by 4. This will lead to a weird-sounding time signature.");
	}

	private String readAlphabet(String alphabet) {
		// read alphabet
		System.out.print("\nAlphabet: " + alphabet.charAt(0));
		for (int i = 1; i < alphabet.length(); i++)
			System.out.print(", " + alphabet.charAt(i));
		return alphabet;
	}

	private void readGrid(String grid, String alphabet) {
		// read grid and convert to midi
		int counter, lCounter, note, atMeasure;

		if (hasModifier) {
			boolean hasSpecChar = false, minor;
			int alphIndex = 0, mCounter, cCounter;

			grid = extractModifier(grid);
			char specialChar = findSpecChar(grid);
			if (specialChar != 'z') {
				hasSpecChar = true;
				// remove special char from alphabet and modifier
				String alphabetTemp = "";
				for (int i = 0; i < alphabet.length(); i++) {
					if (alphabet.charAt(i) != specialChar) {
						alphabetTemp = alphabetTemp + alphabet.charAt(i);
					}
				}
				alphabet = alphabetTemp;
				modifier[modifier.length - 1] = modifier[modifier.length - 1]
						.substring(0,
								modifier[modifier.length - 1].length() - 1);
			}
			// midi-shit
			for (int i = 0; i < alphabet.length(); i++)
				tracks.add(new MidiTrack());

			atMeasure = 0;
			for (int i = 0; i < modifier.length; i++) {
				System.out.print("\n\nExecuting modifier: " + modifier[i]);
				System.out.print("\nNow at measure " + atMeasure);
				minor = false;
				mCounter = 0;
				// determine if minor:
				if (hasSpecChar && modifier[i].length() > 0
						&& modifier[i].charAt(0) == specialChar) {
					minor = true;
					mCounter++;
					System.out.print("\nThis section is in minor.");
				} else if (modifier[i].length() > 0)
					System.out.print("\nThis section is in major.");
				// determine first char:
				cCounter = 0;
				if (mCounter < modifier[i].length() && (!hasSpecChar || modifier[i].charAt(mCounter) != specialChar)) {
					for (int j = 0; j < alphabet.length(); j++) {
						if (alphabet.charAt(j) == modifier[i].charAt(mCounter)) {
							alphIndex = j;
							break; // break from for-loop
						}
					}
					cCounter++;
					mCounter++;
				}
				// count first char:
				while (mCounter < modifier[i].length()) {
					if (modifier[i].charAt(mCounter) != alphabet
							.charAt(alphIndex)
							|| mCounter == modifier[i].length() - 1) {
						break;
					}
					cCounter++;
					mCounter++;
				}
				if (modifier[i].length() > 0) {
					System.out.print("\nCounted " + alphabet.charAt(alphIndex)
							+ " " + cCounter + " times.");
					mCounter--; // go back 1 so write notes won't skip first
								// note
				}
				// write notes
				while (mCounter < modifier[i].length()) {
					if (modifier[i].charAt(mCounter) == specialChar)
						break;
					// find position in alphabet:
					for (int k = 0; k < alphabet.length(); k++) {
						if (alphabet.charAt(k) == modifier[i].charAt(mCounter)) {
							alphIndex = k;
							break;
						}
					}
					counter = 0;
					lCounter = 0;
					System.out.print("\nSequence " + alphabet.charAt(alphIndex)
							+ ":");
					System.out.print("\nNote 1:");
					for (int k = 0; k < grid.length(); k++) {
						// System.out.print("\nCounter: "+counter);
						// System.out.print("\nReading char: " +
						// grid.charAt(k));
						if (grid.charAt(k) == alphabet.charAt(alphIndex))
							lCounter++;
						if (grid.charAt(k) == ','
								|| (grid.charAt(k) == alphabet
										.charAt(alphIndex) && k == grid
										.length() - 1)) {
							if (lCounter > 0) {
								note = (nrows - ((counter + 1) % nrows))
										% nrows;
								System.out.print("\nPlaying note " + note
										+ " in octave "
										+ counterToOctave(lCounter));
								for (int l = 0; l < cCounter; l++) {
									tracks.get(alphIndex + 1).insertNote(
											alphIndex,
											noteToPitch(note, lCounter,
													minor ? 1 : 0),
											100,
											(atMeasure + l) * ncols * 480
													+ (counter / nrows) * 480,
											480);
								}
								lCounter = 0;
							}
							if (k != grid.length() - 1) {
								counter++;
								if (counter % nrows == 0) {
									System.out.print("\nNote "
											+ (counter / nrows + 1) + ":");
								}
							}
						}
					}
					mCounter++;
				}
				atMeasure = atMeasure + cCounter;
				if (mCounter < modifier[i].length()
						&& modifier[i].charAt(mCounter) == specialChar) { // indicates
																			// instrument
																			// change
					
					String helper = "";
					char currChar = 'z';
					mCounter++;
					//skip leading zeros:
					while (modifier[i].charAt(mCounter) == specialChar)
						mCounter++;
					while (mCounter < modifier[i].length()) {
						if (currChar == 'z') { // not yet determined
							currChar = modifier[i].charAt(mCounter);
							// find position in alphabet:
							for (int k = 0; k < alphabet.length(); k++) {
								if (alphabet.charAt(k) == modifier[i]
										.charAt(mCounter)) {
									alphIndex = k;
									break;
								}
							}
						}
						if (modifier[i].charAt(mCounter) == currChar) {
							helper = helper + '1';
							mCounter++;
						} else if (modifier[i].charAt(mCounter) == specialChar) {
							helper = helper + '0';
							mCounter++;
						} else {
							if (!helper.equals("")) {
								System.out.print("\nInstrument change string: "+helper);
								changeInstrument(alphIndex+1,Integer.parseInt(helper,2));
							}
							helper = "";
							currChar = 'z';
						}
					}
					if (!helper.equals("")) {
						System.out.print("\nInstrument change string: "+helper);
						changeInstrument(alphIndex+1, Integer.parseInt(helper,2));
					}
				}
			}
		} else {
			// fall back to old method
			for (int i = 0; i < alphabet.length(); i++) {
				counter = 0;
				lCounter = 0;
				tracks.add(new MidiTrack());
				System.out.print("\n\nSequence " + alphabet.charAt(i) + ":");
				System.out.print("\nNote 1:");
				for (int j = 0; j < grid.length(); j++) {
					// System.out.print("\nCounter: "+counter);
					// System.out.print("\nReading char: " + grid.charAt(j));
					if (grid.charAt(j) == alphabet.charAt(i))
						lCounter++;
					if (grid.charAt(j) == ','
							|| (grid.charAt(j) == alphabet.charAt(i) && j == grid
									.length() - 1)) {
						if (lCounter > 0) {
							note = (nrows - ((counter + 1) % nrows)) % nrows;
							System.out
									.print("\nPlaying note " + note
											+ " in octave "
											+ counterToOctave(lCounter));
							tracks.get(i + 1).insertNote(i,
									noteToPitch(note, lCounter, 0), 100,
									i * ncols * 480 + (counter / nrows) * 480,
									480);
							lCounter = 0;
						}
						if (j != grid.length() - 1) {
							counter++;
							if (counter % nrows == 0) {
								System.out.print("\nNote "
										+ (counter / nrows + 1) + ":");
							}
						}
					}
				}
			}
		}
	}

	private String extractModifier(String grid) {
		// read modifier and remove from gridstring;
		modifier = new String[ncols];
		String helper1 = "", helper2 = "", gridTemp = "";
		int counter = 0;

		for (int i = 0; i < grid.length(); i++) {
			if (counter % nrows == nrows - 1) {
				if (grid.charAt(i) != ',')
					helper1 = helper1 + grid.charAt(i);
			} else
				helper2 = helper2 + grid.charAt(i);
			if (grid.charAt(i) == ',') {
				if (counter % nrows == nrows - 1) {
					gridTemp = gridTemp + helper2;
					helper2 = "";
					modifier[counter / nrows] = helper1;
					helper1 = "";
				}
				counter++;
			}
		}
		gridTemp = gridTemp + helper2;
		helper2 = "";
		modifier[counter / nrows] = helper1;
		helper1 = "";
		nrows--;
		return gridTemp;
	}

	private char findSpecChar(String grid) {
		if (modifier[modifier.length - 1].length() < 1)
			return 'z';
		char possSpecChar = modifier[modifier.length - 1]
				.charAt(modifier[modifier.length - 1].length() - 1);
		//System.out.print("\nPossible special char: " + possSpecChar);
		for (int i = 0; i < grid.length(); i++) {
			if (grid.charAt(i) == possSpecChar) {
				System.out.print("\nSpecial char not found");
				return 'z'; // special char not found
			}
		}
		System.out.print("\nSpecial char found: " + possSpecChar);
		return possSpecChar; // special char found
	}

	static int counterToOctave(int counter) {
		if (counter % 2 == 0)
			return counter / 2;
		else
			return -(counter / 2);
	}

	private int noteToPitch(int note, int lCounter, int scaleSwitch) {
		int octave = 0, pitch;
		int majorScale[] = { 0, 2, 4, 5, 7, 9, 11 };
		int minorScale[] = { 0, 2, 3, 5, 7, 8, 10 };

		octave = counterToOctave(lCounter);
		if (scaleSwitch == 1)
			pitch = rootNote + minorScale[note % 7] + (int) (note / 7) * 12
					+ octave * 12;
		else
			pitch = rootNote + majorScale[note % 7] + (int) (note / 7) * 12
					+ octave * 12;
		// System.out.print("\nReturning pitch "+pitch);

		return pitch;
	}

	private void changeInstrument(int track, int instrNr) {
		String instrName[] = { "piano", "violin", "cello", "flute", "trumpet",
				"saxophone", "guitar", "bass", "sawtooth", "organ", "strings",
				"choir", "xylophone", "banjo", "steeldrum", "helicopter" };
		int instr[] = { 0, 40, 42, 73, 56, 65, 24, 34, 81, 19, 48, 91, 13, 105,
				114, 125 };
		//copy and delete all events, then change instrument, then copy all events back (to make sure instrument change comes first)
		TreeSet<MidiEvent> mEventsTemp = (TreeSet<MidiEvent>) tracks.get(track).getEvents().clone();
		tracks.get(track).getEvents().clear();
		tracks.get(track).insertEvent(new ProgramChange(0, track - 1, instr[instrNr]));
		tracks.get(track).getEvents().addAll(mEventsTemp);
		System.out.print("\nChanged track " + track + " to "
				+ instrName[instrNr]);
	}

	public void makeMidi(File output) {
		// midi-shit
		MidiFile midi = new MidiFile(MidiFile.DEFAULT_RESOLUTION, tracks);
		try {
			midi.writeToFile(output);
		} catch (IOException e) {
			System.err.println(e);
		}
	}

  public void makeMidi() {
    makeMidi(new File("out.mid"));
  }
}

