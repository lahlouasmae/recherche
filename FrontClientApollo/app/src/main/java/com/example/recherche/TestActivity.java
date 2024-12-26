package com.example.recherche;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class TestActivity extends AppCompatActivity {
    private static final String SERVER_URL = "http://10.0.2.2:8082/graphql"; // Pour l'émulateur Android
    private GraphQLPerformanceTest performanceTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        performanceTest = new GraphQLPerformanceTest(this, SERVER_URL);

        findViewById(R.id.btnStartTest).setOnClickListener(v -> startTests());
    }

    private void startTests() {
        Toast.makeText(this, "Starting performance tests...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            performanceTest.runAllTests();
            runOnUiThread(() -> {
                Toast.makeText(this, "Tests completed! Check results in app files.", Toast.LENGTH_LONG).show();
            });
        }).start();
    }
}