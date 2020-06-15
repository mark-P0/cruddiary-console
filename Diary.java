import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

class CrudeDiary {
    final String EMBEDDED_DATABASE_FILEPATH = "crud.db";
    final String TABLE_NAME = "entries";
    final List<Object> HEADERS = new ArrayList<>();
    final SQLiteConnection EMBEDDED_DATABASE = new SQLiteConnection(new File(EMBEDDED_DATABASE_FILEPATH));

    int LARGEST_HEADER_LENGTH = 0;

    public CrudeDiary() {
        // Opens the database
        try {
            this.EMBEDDED_DATABASE.open(true);
        } catch (SQLiteException error) {
            System.out.printf(
                "An exception has occurred while opening file '%s' [%s]\n", 
                this.EMBEDDED_DATABASE_FILEPATH, error);
            System.exit(1);
        }

        // Retrieve table names
        try {
            String queryTableNames = "SELECT name FROM sqlite_master WHERE type='table'";
            SQLiteStatement statement = this.EMBEDDED_DATABASE.prepare(queryTableNames);
            
            List<String> tables = new ArrayList<>();
            while (statement.step()) {
                tables.add((String) statement.columnValue(0));   
            }

            statement.dispose();
            
            if (!tables.contains(this.TABLE_NAME)) {
                String queryCreateTable = "CREATE TABLE 'entries' ('Number'	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,'Entry' TEXT,'Date'	TEXT,'Time'	TEXT)";
                SQLiteStatement statem_ = this.EMBEDDED_DATABASE.prepare(queryCreateTable);

                statem_.stepThrough();
                statem_.dispose();
            }

        } catch (Exception error) {
            System.out.printf(
                "An exception has occurred while accessing table '%s' [%s]\n", 
                this.TABLE_NAME, error);
            System.exit(1);
        }
        
        // Retrieves column headers
        try {
            String queryHeaders = String.format("PRAGMA table_info(%s)", this.TABLE_NAME);
            SQLiteStatement statement = this.EMBEDDED_DATABASE.prepare(queryHeaders);

            while (statement.step()) {
                String item = (String) statement.columnValue(1);
                this.HEADERS.add(item);

                if (item.length() > this.LARGEST_HEADER_LENGTH) {
                    this.LARGEST_HEADER_LENGTH = item.length();
                }
            }
        } catch (SQLiteException error) {
            System.out.printf("[Error] %s\n", error);
        }
    }

    public String toString() {
        return "Crude diary!";
    }
    
    public int displaySpecificEntry(Scanner scanner) {
        int num = -1;

        try {
            String queryShowSpecificEntry = String.format("SELECT * FROM %s WHERE Number=?", this.TABLE_NAME);
            SQLiteStatement statement = this.EMBEDDED_DATABASE.prepare(queryShowSpecificEntry);

            num = InputWrapper.prompt(scanner, "Enter entry number: ", Integer.class);
            statement.bind(1, num);
            
            List<Object> row = new ArrayList<>();            
            boolean hasData = false;
            while (statement.step()) {
                row = new ArrayList<>();
                
                for (int index = 0; index < statement.columnCount(); index++) {
                    row.add(statement.columnValue(index));
                }
                
                hasData = true;
            }
            
            statement.dispose();
            if (!hasData) {
                InputWrapper.prompt(scanner, "Your input may be invalid. Press Enter to continue. . . ", String.class);
                return -1;
            }

            this.displaySpecificRow(this.HEADERS);
            this.displaySpecificRow(row);
            System.out.println("");
        } catch (Exception error) {
            System.out.printf("[Error] %s\n", error);
        }

        return num;
    }

    public void displaySpecificRow(List<Object> entry) {
        String line = "";

        for (int index = 0; index < entry.size(); index++) {
            int allowance = this.LARGEST_HEADER_LENGTH + (
                index == 0 ? 0 :
                index == 1 ? 35 :
                index == 2 ? 15 : 10
            );
            
            String item = String.format("%s", entry.get(index));
            if (item.equals("Number")) {
                item = "#";
            }

            int offset = 5;
            if (item.length() > allowance - offset && index == 1) {
                item = item.substring(0, allowance - offset) + "...";
            }

            line += String.format("%1$-" + allowance + "s", item);
        }

        System.out.println(line);
    }

    public void getEntries() {
        List<List<Object>> rows = new ArrayList<>();
        
        // Column names
        rows.add(this.HEADERS);
        
        // Will produce a list of rows
        try {
            // Prepares query for getting all values
            String queryRawEntries = String.format("SELECT * FROM %s", this.TABLE_NAME);
            SQLiteStatement statement = this.EMBEDDED_DATABASE.prepare(queryRawEntries);

            // Accesses values in each row
            while (statement.step()) {
                List<Object> row = new ArrayList<>();

                for (int index = 0; index < statement.columnCount(); index++) {
                    row.add(statement.columnValue(index));
                }

                rows.add(row);
            }
            
            // Ends query statement
            statement.dispose();
        } catch (Exception error) {
            // System.out.println(String.format(
            //     "An error has occurred: %s", error));

            System.out.printf("[Error] %s\n", error);
        }
        
        // Processes the list of entries
        for (List<Object> entry : rows) {
            this.displaySpecificRow(entry);
        }
    }

    public void addEntry(Map<String, String> newEntry) {        
        List<String> limitedHeaderList = new ArrayList<>();
        List<String> placeholderList = new ArrayList<>();

        for (Object header : this.HEADERS) {
            String head_ = (String) header;

            if (head_.equals("Number")) {
                continue;
            }

            if (!newEntry.containsKey(head_)) {
                newEntry.put(head_, "NULL");
            }

            limitedHeaderList.add(head_);
            placeholderList.add("?");            
        }

        String limitedHeaders = String.join(",", limitedHeaderList);
        String placeholders = String.join(",", placeholderList);

        try {
            String queryNewEntry = String.format(
                "INSERT INTO '%s'(%s) VALUES (%s)", 
                this.TABLE_NAME, limitedHeaders, placeholders
            );
            SQLiteStatement statement = this.EMBEDDED_DATABASE.prepare(queryNewEntry);

            for (int index = 0; index < statement.getBindParameterCount(); index++) {
                String mapKey = limitedHeaderList.get(index);
                statement.bind(index + 1, newEntry.get(mapKey));                
            }

            // Execute query statement
            statement.stepThrough();

            // Ends query statement
            statement.dispose();

        } catch (Exception error) {
            System.out.printf("[Error] %s\n", error);
        }

    }

    public void addEntry(Scanner scanner) {
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy, MMMM dd");
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("hh:mm a");
        
        Map<String, String> info = Map.of(
            "Entry", InputWrapper.prompt(scanner, "Enter new entry: ", String.class),
            "Date", currentDateTime.format(dateFormat),
            "Time", currentDateTime.format(timeFormat)
        );

        this.addEntry(info);
    }

    public void editEntry(int entryNumber, String updatedEntry) {
        try {
            String queryEditEntry = String.format("UPDATE '%s' SET Entry=? WHERE _rowid_=?", this.TABLE_NAME);
            SQLiteStatement statement = this.EMBEDDED_DATABASE.prepare(queryEditEntry);

            statement.bind(1, updatedEntry);
            statement.bind(2, entryNumber);

            statement.stepThrough();
            statement.dispose();            
        } catch (Exception error) {
            System.out.printf("[Error] %s\n", error);
        }
    }

    public void editEntry(Scanner scanner) {
        int number = this.displaySpecificEntry(scanner);
        if (number < 0) {
            return;
        }

        String changeEntryTo = InputWrapper.prompt(scanner, "Change entry to: ", String.class);
        this.editEntry(number, changeEntryTo);
    }

    public void deleteEntry(int entryNumber) {
        try {
            String queryEditEntry = String.format("DELETE FROM 'entries' WHERE _rowid_=?", this.TABLE_NAME);
            SQLiteStatement statement = this.EMBEDDED_DATABASE.prepare(queryEditEntry);

            statement.bind(1, entryNumber);

            statement.stepThrough();
            statement.dispose();
        } catch (Exception error) {
            System.out.printf("[Error] %s\n", error);
        }

    }

    public void deleteEntry(Scanner scanner) {
        int number = this.displaySpecificEntry(scanner);
        if (number < 0) {
            return;
        }

        String userIsSure = InputWrapper.prompt(scanner, "Are you absolutely sure you want to delete this entry? [Y]: ", String.class);
        if (userIsSure.equals("Y")) {
            this.deleteEntry(number);
        }
    }

    public void closeDatabase() {
        this.EMBEDDED_DATABASE.dispose();
    }
}

public class Diary {
    private static void console() {
        String menuMessage = "";
        List<Integer> options = new ArrayList<Integer>();
        List<String> messages = List.of(
            "Add entry",
            "Edit entry",
            "Delete entry",
            "End application"
        );

        for (int index = 0; index < messages.size(); index++) {
            int selection = (index == messages.size() - 1) ? 0 : index + 1;
            String message = messages.get(index);

            options.add(selection);
            menuMessage += String.format(
                "%s | %s%s", selection, message, (selection == 0) ? "" : "\n"
            );
        }

        Scanner inputScanner = new Scanner(System.in);
        CrudeDiary diary = new CrudeDiary();

        boolean shouldPersist = true;
        while (shouldPersist) {
            System.out.println(
                new String(new char[50]).replace("\0", "-") + "\n" +
                diary
            );

            diary.getEntries();
            System.out.println("");

            System.out.println("[OPTIONS]\n" + menuMessage);
            int selection = InputWrapper.prompt(inputScanner, "Selection: ", int.class);
            System.out.println("");

            switch (selection) {
                case 1:
                    diary.addEntry(inputScanner);
                    break;

                case 2:
                    diary.editEntry(inputScanner);
                    break;

                case 3:
                    diary.deleteEntry(inputScanner);
                    break;

                case 0:
                    shouldPersist = false;
                    break;
            
                default:
                    System.out.println("You have made an invalid selection!");
                    break;
            }

            System.out.println("");
        }
        
        System.out.println("Thank you for using the app!");
        diary.closeDatabase();
        inputScanner.close();
    }

    // private static void test() {
    //     // int length = 10;
    //     // String inputString = "Fuck";

    //     // String test = String.format("%1$-" + length + "s", inputString);

    //     // System.out.println(test);

    // }

    public static void main(String[] args) {
        console();
        
        // test();
    }
}