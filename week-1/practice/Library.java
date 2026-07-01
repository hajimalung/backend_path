abstract class LibraryItem {
    private String title;
    private int id;

    LibraryItem(String title, int id) {
        this.title = title;
        this.id = id;
    }

    public abstract String getType();

    public String getTitle() {
        return title;
    }

    public int getId() {
        return id;
    }

    public void display() {
        System.out.println("[" + this.id + "] " + this.title + " (" + this.getType() + ")");
    }
}

interface Barrowable {
    void borrow();

    default void returnItem() {
        System.out.println("Returning item");
    }
}

class Book extends LibraryItem implements Barrowable {
    private String author;

    Book(String title, int id, String author) {
        super(title, id);
        this.author = author;
    }

    @Override
    public String getType() {
        return "Book";
    }

    @Override
    public void borrow() {
        System.out.println("Borrowing book: " + this.getTitle() + " by " + this.author);
    }

    @Override
    public void returnItem() {
        System.out.println("Returning book: " + this.getTitle() + " by " + this.author);
    }
}

public class Library {
    public static void main(String[] args) {
        Book book1 = new Book("The Great Gatsby", 1, "F. Scott Fitzgerald");
        book1.display();
        book1.borrow();
        book1.returnItem();
    }
}