package com.example.androidcicd.movie;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.util.ArrayList;

public class MovieProvider {
    private static MovieProvider movieProvider;
    private final ArrayList<Movie> movies;
    private final CollectionReference movieCollection;

    public MovieProvider(FirebaseFirestore firestore) {
        movies = new ArrayList<>();
        movieCollection = firestore.collection("movies");
    }

    public interface DataStatus {
        void onDataUpdated();
        void onError(String error);
    }

    // Singleton instance getter
    public static MovieProvider getInstance(FirebaseFirestore firestore) {
        if (movieProvider == null) {
            movieProvider = new MovieProvider(firestore);
        }
        return movieProvider;
    }

    // Listen for updates in the movies collection
    public void listenForUpdates(final DataStatus dataStatus) {
        movieCollection.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                dataStatus.onError(error.getMessage());
                return;
            }
            movies.clear();
            if (snapshot != null) {
                for (QueryDocumentSnapshot item : snapshot) {
                    movies.add(item.toObject(Movie.class));
                }
                dataStatus.onDataUpdated();
            }
        });
    }

    // Get the list of movies
    public ArrayList<Movie> getMovies() {
        return movies;
    }

    // Update movie details
    public void updateMovie(Movie movie, String title, String genre, int year) {
        movie.setTitle(title);
        movie.setGenre(genre);
        movie.setYear(year);
        DocumentReference docRef = movieCollection.document(movie.getId());
        if (validMovie(movie, docRef)) {
            docRef.set(movie);
        } else {
            throw new IllegalArgumentException("Invalid Movie!");
        }
    }

    // Add a new movie
    public void addMovie(Movie movie) {
        DocumentReference docRef = movieCollection.document();
        movie.setId(docRef.getId());
        if (validMovie(movie, docRef)) {
            docRef.set(movie);
        } else {
            throw new IllegalArgumentException("Invalid Movie!");
        }
    }

    // Delete an existing movie
    public void deleteMovie(Movie movie) {
        DocumentReference docRef = movieCollection.document(movie.getId());
        docRef.delete();
    }

    // Validate a movie's properties
    public boolean validMovie(Movie movie, DocumentReference docRef) {
        return movie.getId().equals(docRef.getId()) && !movie.getTitle().isEmpty() && !movie.getGenre().isEmpty() && movie.getYear() > 0;
    }

    // Check if a movie with a given title already exists in the database
    public Task<Boolean> movieExists(String title) {
        TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();

        movieCollection.whereEqualTo("title", title).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        taskCompletionSource.setResult(true);  // Movie exists
                    } else {
                        taskCompletionSource.setResult(false); // Movie does not exist
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MovieProvider", "Error checking movie existence", e);
                    taskCompletionSource.setResult(false);  // Assume movie doesn't exist if error occurs
                });

        return taskCompletionSource.getTask();  // Return the task for async handling
    }
}
