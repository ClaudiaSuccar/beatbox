import java.awt.*;
import javax.swing.*;
import javax.sound.midi.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.io.*;
import java.net.*;

public class BeatBox {

  JPanel mainPanel; // mainPanel that holds all the checkboxes
  ArrayList<JCheckBox> checkboxList; // a list of the checkboxes
  Sequencer sequencer; // sequencer for music
  Sequence sequence;
  Sequence mySequence = null; // stores user's sequence to send
  Track track;
  JFrame theFrame; // the frame everything is placed on
  JList incomingList; // displays incoming messages
  JTextField userMessage; // textbox for user to send message
  int nextNum; // stores number of user messages (starting from 0)
  Vector<String> listVector = new Vector<String>(); // deprecated but needed for JList data
  String userName; // hold username passed into command-line argument
  ObjectOutputStream out; // initialize output stream
  ObjectInputStream in; // initialize input stream
  HashMap<String, boolean[]> otherSeqsMap = new HashMap<String, boolean[]>();
  // map that is used to send the user message and the array list of checkbox boolean states

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
    try {
      new BeatBox().startUp(args[0]); // args[0] is the user ID/screen name (REQUIRES command-line argument)
    } catch (Exception ex) {
      System.out.println("Missing command-line argument for userName. 'Example: java BeatBox coolcat'");
    }
  }

  public void startUp(String name) {
    userName = name; // sets userName from command-line argument
    try { // attempts to connect to server
      Socket socket = new Socket("127.0.0.1", 4242);
      out = new ObjectOutputStream(socket.getOutputStream()); // for output to server
      in = new ObjectInputStream(socket.getInputStream()); // for input from server
      Thread remote = new Thread(new RemoteReader()); // thread to read from server
      remote.start();
    } catch (Exception ex) {
      System.out.println("Failed to connect to server.");
    }
    setUpMidi(); // sets the default actions for sequencer / sequence / track
    buildGUI(); // builds the user interface
  }

  public void buildGUI() {
    theFrame = new JFrame("Virtual Beat Box"); // creates a window titled with cyber BeatBox
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
    serializeIt.addActionListener(new MySaveListener());
    buttonBox.add(serializeIt);

    JButton restore = new JButton("Load");
    restore.addActionListener(new MyReadInListener());
    buttonBox.add(restore);

    incomingList = new JList(); // instantiate a list for all messages
    incomingList.addListSelectionListener(new MyListSelectionListener()); // associate list with MyListSelectionListener event
    incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // set selection mode for the JList (to load and play the attached track)
    JScrollPane theList = new JScrollPane(incomingList); //  create ScrollPanel to hold all the messages
    buttonBox.add(theList); // add the scroll panel to the button area
    incomingList.setListData(listVector); // set the data of the list to the list vector of strings

    userMessage = new JTextField(); // instantiate text field for user's message
    buttonBox.add(userMessage); // add the text box to the buttons

    JButton sendMessage = new JButton("Send Message");
    sendMessage.addActionListener(new MySendMessageListener());
    buttonBox.add(sendMessage);

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
    ArrayList<Integer> trackList = null; // holds the instruments for each

    sequence.deleteTrack(track); //clears the track
    track = sequence.createTrack(); //sets the track to a new one

    for (int i = 0; i < 16; i++) { // iterate through each row of the checkboxes
      trackList = new ArrayList<Integer>(); //set tracklist to an array list object


      for (int j = 0; j < 16; j++) { // iterate through the items in each row
        JCheckBox jc = (JCheckBox) checkboxList.get(j + (16*i)); // get state for each checkbox item
        if(jc.isSelected()) { // if the checkBox is selected, set it to its corresponding instrument
          int key = instruments[i]; // set the key equal to its corresponding instrument
          trackList.add(new Integer(key));
        } else {
          trackList.add(null); // else set it to null (no sound)
        }
      }

      makeTracks(trackList); // make the track using the new trackList
      // track.add(makeEvent(176, 1, 127, 0, 16)); // make events for all 16 beats
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

  public void makeTracks(ArrayList list) { // called in buildTrackAndStart() method, takes the newlist as an argument
    Iterator it = list.iterator();
    for (int i = 0; i < 16; i++) { // iterates through the newlist
      Integer num = (Integer) it.next(); // reads through each item on the list
      if(num != null) { // if the key is not set to 0, note on and off event are added to the track
        int numKey = num.intValue(); // get the value of that key
        track.add(makeEvent(144, 9, numKey, 100, i));
        track.add(makeEvent(128, 9, numKey, 100, i+1));
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

  public class MySaveListener implements ActionListener {
    public void actionPerformed(ActionEvent a) {
      boolean[] checkboxState = new boolean[256]; // saves the STATE of the checkboxes
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

public class MySendMessageListener implements ActionListener {
  public void actionPerformed(ActionEvent a) {
    boolean[] checkboxState = new boolean[256];
    for(int i = 0; i < 256; i++) {
      JCheckBox check = (JCheckBox) checkboxList.get(i); // get checkBox states
      if (check.isSelected()) {
        checkboxState[i] = true;
      }
    }
    String messageToSend = null; // initialize message to send to other users
    try {
      out.writeObject(userName + nextNum++ + ": " + userMessage.getText()); // writes the message to the server, using the username
      out.writeObject(checkboxState); // writes the state of checkboxes for other users to load
    } catch (Exception ex) {
      System.out.println("Error. Connection to server was lost.");
    }
    userMessage.setText(""); // reset the text box after submission
  }
}

public class MyListSelectionListener implements ListSelectionListener { // whene a user selected a message to load
  public void valueChanged(ListSelectionEvent le) {
    if(!le.getValueIsAdjusting()) {
      String selected = (String) incomingList.getSelectedValue();
      if (selected != null) {
        //go to the map and change the sequence
        boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected); // get associated beat pattern in the hash map otherSeqsMap
        changeSequence(selectedState); // load the associated beat pattern
        sequencer.stop();
        buildTrackAndStart(); // start playing the new sequence
      }
    }
  }
}

public class RemoteReader implements Runnable { // thread that reads data from the server, called in startUp method
  boolean[] checkboxState = null; // initialize state, name to display, and the obj (array list of checkboxes)
  String nameToShow = null;
  Object obj = null;
  public void run() { // first method to run when thread is running
    try {
      while ((obj=in.readObject()) != null) { // while there is an object to read
        System.out.println("Received object from server.");
        System.out.println(obj.getClass()); // get the object's class informaiton
        // deserialize the object states (message and arraylist of checkbox states (boolean))
        String nameToShow = (String) obj; // set the name to show to the object's name
        checkboxState = (boolean[]) in.readObject(); // cast boolean array of input steam's read object
        otherSeqsMap.put(nameToShow, checkboxState); // add name and object state to the hash map
        listVector.add(nameToShow); // add the name of object to list vector of strings
        incomingList.setListData(listVector); // add new data to the incoming list
      }
    } catch (Exception ex) { ex.printStackTrace(); }
  }
}

public class MyPlayMineListener implements ActionListener {
  public void actionPerformed(ActionEvent a) {
    if (mySequence != null) {
      sequence = mySequence; // restore to user's original sequence
    }
  }
}

public void changeSequence(boolean[] checkboxState) { // immediately loads the selected pattern from the list (called in MyListSelectionListener)
  for (int i = 0; i < 256; i++) { // go through each checkbox
    JCheckBox check = (JCheckBox) checkboxList.get(i); // get the state of the checkbox
    if (checkboxState[i]) { // if the checkbox is true
      check.setSelected(true); // set the checkBox object to checked
    } else {
      check.setSelected(false); // else set to unchecked
    }
  }
}

  public class MyReadInListener implements ActionListener { // loads a saved pattern from the file directory
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
