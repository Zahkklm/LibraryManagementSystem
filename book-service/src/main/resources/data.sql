-- Clear any existing data
DELETE FROM books;

-- Classic Literature
INSERT INTO books (title, author, isbn, publisher, publication_date, genre, total_copies, available_copies) 
VALUES ('Pride and Prejudice', 'Jane Austen', '9780141439518', 'Penguin Classics', DATE '1813-01-28', 'Classic Literature', 15, 5);

INSERT INTO books (title, author, isbn, publisher, publication_date, genre, total_copies, available_copies) 
VALUES ('To Kill a Mockingbird', 'Harper Lee', '9780446310789', 'Grand Central', DATE '1960-07-11', 'Classic Literature', 8, 8);

INSERT INTO books (title, author, isbn, publisher, publication_date, genre, total_copies, available_copies) 
VALUES ('1984', 'George Orwell', '9780451524935', 'Signet Classics', DATE '1949-06-08', 'Classic Literature', 10, 10);

INSERT INTO books (title, author, isbn, publisher, publication_date, genre, total_copies, available_copies) 
VALUES ('The Great Gatsby', 'F. Scott Fitzgerald', '9780743273565', 'Scribner', DATE '1925-04-10', 'Classic Literature', 7, 7);

INSERT INTO books (title, author, isbn, publisher, publication_date, genre, total_copies, available_copies) 
VALUES ('Moby Dick', 'Herman Melville', '9780142437247', 'Penguin Classics', DATE '1851-10-18', 'Classic Literature', 4, 4);

INSERT INTO books (title, author, isbn, publisher, publication_date, genre, total_copies, available_copies) 
VALUES ('War and Peace', 'Leo Tolstoy', '9781400079988', 'Vintage', DATE '1869-01-01', 'Classic Literature', 3, 3);

INSERT INTO books (title, author, isbn, publisher, publication_date, genre, total_copies, available_copies) 
VALUES ('The Odyssey', 'Homer', '9780140268867', 'Penguin Classics', DATE '1900-01-01', 'Classic Literature', 6, 6);

INSERT INTO books (title, author, isbn, publisher, publication_date, genre, total_copies, available_copies) 
VALUES ('Crime and Punishment', 'Fyodor Dostoevsky', '9780486415871', 'Dover Publications', DATE '1866-01-01', 'Classic Literature', 5, 5);

INSERT INTO books (title, author, isbn, publisher, publication_date, genre, total_copies, available_copies) 
VALUES ('Jane Eyre', 'Charlotte Brontë', '9780141441146', 'Penguin Classics', DATE '1847-10-16', 'Classic Literature', 7, 7);

INSERT INTO books (title, author, isbn, publisher, publication_date, genre, total_copies, available_copies) 
VALUES ('Wuthering Heights', 'Emily Brontë', '9780141439556', 'Penguin Classics', DATE '1847-12-01', 'Classic Literature', 6, 6);

-- Science Fiction
INSERT INTO books (title, author, isbn, publisher, publication_date, genre, total_copies, available_copies) 
VALUES ('Dune', 'Frank Herbert', '9780441172719', 'Ace', DATE '1965-08-01', 'Science Fiction', 10, 10);

INSERT INTO books (title, author, isbn, publisher, publication_date, genre, total_copies, available_copies) 
VALUES ('Foundation', 'Isaac Asimov', '9780553293357', 'Bantam Spectra', DATE '1951-05-01', 'Science Fiction', 8, 8);

INSERT INTO books (title, author, isbn, publisher, publication_date, genre, total_copies, available_copies) 
VALUES ('Neuromancer', 'William Gibson', '9780441569595', 'Ace', DATE '1984-07-01', 'Science Fiction', 7, 7);

INSERT INTO books (title, author, isbn, publisher, publication_date, genre, total_copies, available_copies) 
VALUES ('Snow Crash', 'Neal Stephenson', '9780553380958', 'Bantam Books', DATE '1992-06-01', 'Science Fiction', 6, 6);

INSERT INTO books (title, author, isbn, publisher, publication_date, genre, total_copies, available_copies) 
VALUES ('The Left Hand of Darkness', 'Ursula K. Le Guin', '9780441478125', 'Ace', DATE '1969-03-01', 'Science Fiction', 5, 5);