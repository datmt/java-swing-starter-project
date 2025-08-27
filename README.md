# **Java Swing & FlatLaf: Modern Desktop App Development**

This curriculum is designed to take a student from the basics of creating a window to building a full-featured, professional-looking, and data-persistent desktop application.

### **Module 1: The Foundation \- Windows & Panels 🖼️**

This module sets the stage. Before you can add buttons or lists, you need a place to put them.

* **JFrame**: The main window of your application. Covers creating it, setting its size, title, close operation, and making it visible.  
* **JPanel**: The fundamental container. Think of it as a canvas you place inside the window where you'll add all other components.  
* **FlatLaf Setup**: How to initialize and apply the FlatLaf look and feel right at the start of your application (FlatLaf.setup(...)).  
* **The Swing Threading Rule**: A crucial concept explaining SwingUtilities.invokeLater to ensure UI updates are safe.

### **Module 2: Core Interactive Components 🕹️**

These are the absolute essentials for user interaction that you'll find in almost every single application.

* **JLabel**: For displaying static text and icons.  
* **JButton**: For triggering actions. Includes text, icons, and handling clicks (ActionListener).  
* **Text Inputs**:  
  * **JTextField**: For single-line text input.  
  * **JPasswordField**: For sensitive password input.  
  * **JTextArea**: For multi-line text input.  
* **Choice Components**:  
  * **JCheckBox**: For on/off toggles.  
  * **JRadioButton**: For selecting one option from a group (requires a ButtonGroup).  
  * **JComboBox**: A dropdown list for selecting an item.

### **Module 3: Displaying Collections of Data 📊**

This module focuses on components designed to handle lists, tables, and hierarchical data—the backbone of data-driven apps.

* **JList**: For displaying a scrollable list of items. Covers using a DefaultListModel.  
* **JTable**: For displaying data in a grid of rows and columns. Covers the DefaultTableModel.  
* **JTree**: For displaying hierarchical data, like files and folders.  
* **JScrollPane**: A crucial container used to add scrollbars to components like JList, JTable, and JTextArea.

### **Module 4: Professional Layouts with MigLayout 📐**

This is a critical module for creating clean, responsive, and maintainable user interfaces without the complexity of older layout managers.

* **Introduction to MigLayout**: Why it's the modern choice for Swing layouts.  
* **Adding the Library**: How to add the MigLayout dependency to your project.  
* **Core Concepts**: Understanding layout constraints, component constraints, and column/row constraints.  
* **Practical Examples**:  
  * Building a simple login form.  
  * Creating a complex settings panel with aligned labels and inputs.  
  * Using keywords like wrap, span, grow, and push to control component behavior.

### **Module 5: Feedback and Range Selection 🌡️**

These components provide visual feedback to the user or allow them to select a value from a continuous range.

* **JProgressBar**: Shows the progress of a task (both determinate and indeterminate).  
* **JSlider**: Allows the user to select a numeric value by sliding a knob.  
* **JSpinner**: An input field with small up and down arrows for stepping through values.  
* **JToolTip**: The hover-text that provides hints to the user.

### **Module 6: Menus and Toolbars 🛠️**

These components provide standard navigation and action controls, typically at the top of the main window.

* **JMenuBar**, **JMenu**, **JMenuItem**: Building the main application menu (File, Edit, etc.).  
* **JPopupMenu**: Creating context menus that appear on a right-click.  
* **JToolBar**: A bar for holding buttons with icons for common actions.

### **Module 7: Dialogs \- Communicating with the User 💬**

Dialogs are pop-up windows used to notify the user of something or to get a specific piece of information.

* **JOptionPane**: For creating simple, standard dialogs (message, confirm, input).  
* **JDialog**: The base for creating fully custom dialog windows (e.g., a "Settings" window).  
* **JFileChooser** & **JColorChooser**: Built-in dialogs for selecting files and colors.

### **Module 8: Storing User Data & Settings 💾**

An application isn't complete until it can remember user data between sessions. This module covers the best methods for data persistence.

* **Simple Settings with java.util.prefs.Preferences**: The ideal, built-in way to store simple key-value data like window size or theme choice.  
* **Structured Data with JSON Files**: Using a library like **Jackson** or **Gson** to save and load application data (e.g., a list of to-do items) to a human-readable .json file.  
* **Robust Data with Embedded Databases (SQLite)**: An introduction to using an embedded SQL database for large, relational datasets that require powerful querying capabilities.

### **Module 9: Power-Ups with FlatLaf ✨**

This final module focuses on the special features and modern enhancements that the FlatLaf library provides to make your app look and feel truly modern.

* **Theming**: Switching between light, dark, and other built-in themes.  
* **Customization**: Using FlatLaf.setGlobalExtraDefaults to tweak colors and styles.  
* **SVG Icons**: Using FlatSVGIcon for sharp, scalable vector icons.  
* **Component-Specific Enhancements**: Placeholder text, tri-state checkboxes, modern styling for tabs, and custom window decorations.
