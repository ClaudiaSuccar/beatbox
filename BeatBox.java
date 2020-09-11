import java.awt.*;
import javax.swing.*;
import javax.sound.midi.*;
import java.util.*;
import java.awt.event.*;
import java.io.*;

public class BeatBox {

  JPanel mainPanel; // mainPanel that holds all the checkboxes
  ArrayList<JCheckBox> checkboxList; // a list of the checkboxes
  Sequencer sequencer; // sequencer for music
  Sequence sequence;
  Track track;
  JFrame theFrame; // the frame everything is placed on

  String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal",
                              "Hand Clap", "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell",
                              "Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga"}; // list of the instrument names in order
  int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63}; //actual numbers used to call the instruments

  public static void main(String[] args) {
    try { // set look and feel to os system's
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (UnsupportedLookAndFeelException e) {e.printStackTrace();}
    catch (ClassNotFoundException e) {e.printStackTrace();}
    catch (InstantiationException e) {e.printStackTrace();}
    catch (IllegalAccessException e) {e.printStackTrace();}
    new BeatBox().buildGUI();
  }

  public void buildGUI() {
    theFrame = new JFrame("Cyber BeatBox"); // creates a window titled with cyber BeatBox
    theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // sets default operation for closing
    BorderLayout layout = new BorderLayout(); // uses BorderLayout
    JPanel background = new JPanel(layout); // creates a new jpanel to hold the layouts
    background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // sets the borders around the layout

    checkboxList = new ArrayList<JCheckBox>(); // sets the checkbox list to a new ArrayList of  JCheckBoxes
    Box buttonBox = new Box(BoxLayout.Y_AXIS); // uses a box layout for the buttons, set to vertical

    JButton start = new JButton("Start"); // creates a start button
    start.addActionListener(new MyStartListener()); // associated the start button with the startlistener event
    buttonBox.add(start); // adds the button to the buttonBox layout

    JButton stop = new JButton("Stop");
    stop.addActionListener(new MyStopListener());
    buttonBox.add(stop);

    JButton upTempo = new JButton("Tempo Up");
    upTempo.addActionListener(new MyUpTempoListener());
    buttonBox.add(upTempo);

    JButton downTempo = new JButton("Tempo Down");
    downTempo.addActionListener(new MyDownTempoListener());
    buttonBox.add(downTempo);

    JButton serializeIt = new JButton("Save");
    serializeIt.addActionListener(new MySendListener());
    buttonBox.add(serializeIt);

    JButton restore = new JButton("Load");
    restore.addActionListener(new MyReadInListener());
    buttonBox.add(restore);

    Box nameBox = new Box(BoxLayout.Y_AXIS); // creates a second box layout to hold the names of the instruments
    for (int i = 0; i < 16; i++) { // iterates through the names and displays them as a label
      nameBox.add(new Label(instrumentNames[i]));
    }

    background.add(BorderLayout.EAST, buttonBox); // assigns the buttonBox to the east region of the BorderLayout
    background.add(BorderLayout.WEST, nameBox); // the names of the instruments will go on the left

    theFrame.getContentPane().add(background); // adds the panel to the jframe pane

    GridLayout grid = new GridLayout(16, 16); // creates a new grid using the grid layout
    grid.setVgap(0); // creates vertical and horizontal gaps
    grid.setHgap(2);
    mainPanel = new JPanel(grid); // sets the mainPanel to the grids (for the checkboxes)
    background.add(BorderLayout.CENTER, mainPanel); // assigns this layout to the center of the pane

    for (int i = 0; i < 256; i++) { // iterates through each checkbox
      JCheckBox c = new JCheckBox(); // create a new checkBox item
      c.setSelected(false); // set the default selected state to false
      checkboxList.add(c); // store each object in the checkBox list
      mainPanel.add(c); // add the checkboxes to the mainPanel
    }

    setUpMidi(); // sets the default actions for sequencer / sequence / track

    theFrame.setBounds(50, 50, 300, 300); // set bounds for the frame
    theFrame.pack(); // pack the frame
    theFrame.setVisible(true); // allow the frame to be seen
  }

  public void setUpMidi() {
    try {
      sequencer = MidiSystem.getSequencer(); // sets up a new sequencer from the MidiSystem
      sequencer.open(); // opens the sequencer
      sequence = new Sequence(Sequence.PPQ, 4); // sets the sequencer parameters
      track = sequence.createTrack(); // creates a new track
      sequencer.setTempoInBPM(120); // sets the tempo at 120 beats per minute
    } catch(Exception e) {e.printStackTrace();} // throws exception and prints stack trace if above fails
  }

  public void buildTrackAndStart() {
    int[] trackList = null; // resets the trackList to null

    sequence.deleteTrack(track); //clears the track
    track = sequence.createTrack(); //sets the track to a new one

    for (int i = 0; i < 16; i++) { // iterate through each row of the checkboxes
      trackList = new int[16]; //set tracklist equal to an array of 16 ints

      int key = instruments[i]; // set the key equal to its corresponding instrument

      for (int j = 0; j < 16; j++) { // iterate through the items in each row

        JCheckBox jc = checkboxList.get(j + 16*i); // create a new checkbox for each item
        if(jc.isSelected()) { // if the checkBox is selected, set it to its corresponding instrument
          trackList[j] = key;
        } else {
          trackList[j] = 0; // else set it to 0 (no sound)
        }
      }

      makeTracks(trackList); // make the track using the new trackList
      track.add(makeEvent(176, 1, 127, 0, 16)); // add the keys to the track
    }

    track.add(makeEvent(192, 9, 1, 0, 15)); // ensures the beatbox reaches full 16 beats
    try {
      sequencer.setSequence(sequence);
      sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY); // allows the music to loop continuously
      sequencer.start(); // plays the sequence!
      sequencer.setTempoInBPM(120);
    } catch(Exception e) {e.printStackTrace();}
  }

  public class MyStartListener implements ActionListener { // set up actionlisteners for all the buttons
    public void actionPerformed(ActionEvent a) {
      buildTrackAndStart();
    }
  }

  public class MyStopListener implements ActionListener {
    public void actionPerformed(ActionEvent a) {
      sequencer.stop();
    }
  }

  public class MyUpTempoListener implements ActionListener {
    public void actionPerformed(ActionEvent a) {
      float tempoFactor = sequencer.getTempoFactor();
      sequencer.setTempoFactor((float) (tempoFactor * 1.03));
    }
  }

  public class MyDownTempoListener implements ActionListener {
    public void actionPerformed(ActionEvent a) {
      float tempoFactor = sequencer.getTempoFactor();
      sequencer.setTempoFactor((float) (tempoFactor * .97));
    }
  }

  public void makeTracks(int[] list) { // called in buildTrackAndStart() method
    // makes events for each instruments based of of the trackList array in buildTrackAndStart() method
    for (int i = 0; i < 16; i++) {
      int key = list[i];

      if(key != 0) { // if the key is not set to 0, note on and off event are added to the track
        track.add(makeEvent(144, 9, key, 100, i));
        track.add(makeEvent(128, 9, key, 100, i+1));
      }
    }
  }

  public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
    // utility method for creating a MidiEvent
    MidiEvent event = null;
    try {
      ShortMessage a = new ShortMessage();
      a.setMessage(comd, chan, one, two);
      event = new MidiEvent(a, tick);
    } catch(Exception e) {e.printStackTrace();}
    return event;
  }

  public class MySendListener implements ActionListener {
    public void actionPerformed(ActionEvent a) {
      boolean[] checkboxState = new boolean[256]; // saves the state of the checkboxes
      for(int i = 0; i < 256; i++) { // walkthrough arraylist of checkboxes and add to boolean arraylist
        JCheckBox check = (JCheckBox) checkboxList.get(i);
        if (check.isSelected()) {
          checkboxState[i] = true; // set to true if selected else false by default
        }
      }

      try {
        FileOutputStream fileStream = new FileOutputStream(new File("Checkbox.ser"));
        ObjectOutputStream os = new ObjectOutputStream(fileStream);
        os.writeObject(checkboxState); // write and serialize the boolean array
      } catch (Exception ex) { ex.printStackTrace(); }
    }
  }

  public class MyReadInListener implements ActionListener {
    public void actionPerformed(ActionEvent a) {
      boolean[] checkboxState = null; // initialize holder for previous checkbox states
      try {
        FileInputStream fileIn = new FileInputStream(new File("Checkbox.ser"));
        ObjectInputStream is = new ObjectInputStream(fileIn);
        checkboxState = (boolean[]) is.readObject(); // cast read object to boolean array, else it will be of type Object
      } catch (Exception ex) { ex.printStackTrace(); }

      for (int i = 0; i < 256; i++) { // go through actual checkboxes and restore their state
        JCheckBox check = (JCheckBox) checkboxList.get(i);
        if (checkboxState[i]) {
          check.setSelected(true);
        } else {
          check.setSelected(false);
        }
      }

      sequencer.stop();
      buildTrackAndStart(); // rebuild sequence using the restored states
    }
  }
}
