package com.saishachhabria.theseefooddiet;

import android.graphics.Bitmap;
import android.graphics.RectF;

import java.util.List;


//Generic interface for interacting with different recognition engines.

public interface Classifier {
    /**
     * An immutable result returned by a Classifier describing what was recognized.
     */
    public class Recognition {
        private final String id; //A unique identifier for what has been recognized. Specific to the class, not the instance ofnthe object.
        private final String title; //Display name for the recognition.
        private final Float confidence; //A sortable score for how good the recognition is relative to others. Higher should be better.
        private RectF location; //Optional location within the source image for the location of the recognized object.

        public Recognition(
                final String id, final String title, final Float confidence, final RectF location) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.location = location;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Float getConfidence() {
            return confidence;
        }

        public RectF getLocation() {
            return new RectF(location);
        }

        public void setLocation(RectF location) {
            this.location = location;
        }

        @Override
        public String toString() {
            String resultString = "";
            if (id != null) {
                resultString += "[" + id + "] ";
            }

            if (title != null) {
                resultString += title + " ";
            }

            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f);
            }

            if (location != null) {
                resultString += location + " ";
            }

            return resultString.trim();
        }
    }

    List<Recognition> recognizeImage(Bitmap bitmap);

    void enableStatLogging(final boolean debug);

    String getStatString();

    void close();
}

