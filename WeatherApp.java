import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.File;
import java.awt.image.BufferedImage;

public class WeatherApp {
    public static void main(String[] args) {
        // Create the frame
        JFrame frame = new JFrame("Weather App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);

        // Custom JPanel to paint the background image
        class BackgroundPanel extends JPanel {
            private BufferedImage backgroundImage;

            public BackgroundPanel() {
                try {
                    backgroundImage = ImageIO.read(new File("images/background.jpg")); // Set your background image here
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        }

        // Create the background panel and set layout
        BackgroundPanel backgroundPanel = new BackgroundPanel();
        backgroundPanel.setLayout(new BorderLayout());

        // Create a panel for input and button, styled for aesthetics
        JPanel inputPanel = new JPanel();
        inputPanel.setOpaque(false); // Make the panel transparent
        inputPanel.setLayout(new GridLayout(3, 1, 10, 10));

        JLabel cityLabel = new JLabel("Enter city name:");
        cityLabel.setForeground(Color.WHITE);  // Text color for contrast with the background
        cityLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        // Create the input field with rounded corners
        JTextField cityField = new JTextField(20) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15); // Set the rounding here
                super.paintComponent(g);
            }
        };
        cityField.setOpaque(false); // Transparent background
        cityField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        cityField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE, 2), // Outer border (white color)
            BorderFactory.createEmptyBorder(5, 10, 5, 10)   // Padding inside the input field
        ));
        cityField.setBackground(new Color(255, 255, 255, 200)); // Light transparent background

        // Create the fetch button with grey color
        JButton fetchButton = new JButton("Fetch Weather");
        fetchButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        fetchButton.setBackground(Color.GRAY);  // Grey background
        fetchButton.setForeground(Color.WHITE); // White text color
        fetchButton.setFocusPainted(false);     // Remove the focus outline

        inputPanel.add(cityLabel);
        inputPanel.add(cityField);
        inputPanel.add(fetchButton);

        // Label for displaying loading icon and weather result
        JLabel loadingLabel = new JLabel("", SwingConstants.CENTER);
        loadingLabel.setForeground(Color.WHITE);
        loadingLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        // Add components to the background panel
        backgroundPanel.add(inputPanel, BorderLayout.NORTH);
        backgroundPanel.add(loadingLabel, BorderLayout.CENTER);

        // Add background panel to frame
        frame.add(backgroundPanel);

        frame.setVisible(true);

        // Action listener for the button click
        fetchButton.addActionListener(e -> {
            String city = cityField.getText().trim();
            if (city.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "City name cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Show loading spinner
            Icon loadingIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage("images/loading.gif"));
            loadingLabel.setIcon(loadingIcon);
            loadingLabel.setText("Fetching data...");

            // Perform the API fetch in a background thread
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    try {
                        String APIKey = ""; // Replace with your OpenWeatherMap API Key
                        String urlString = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&units=metric&appid=" + APIKey;

                        // Create a URL object and open a connection
                        URL url = new URL(urlString);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");

                        // Get the response from the API
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        // Convert the response to a string
                        String jsonResponse = response.toString();

                        // Check if the city is not found (error 404)
                        if (jsonResponse.contains("\"cod\":\"404\"")) {
                            JOptionPane.showMessageDialog(frame, "City not found.", "Error", JOptionPane.ERROR_MESSAGE);
                            return null;
                        }

                        // Extract weather details using simple string parsing
                        String temperature = jsonResponse.split("\"temp\":")[1].split(",")[0];
                        String humidity = jsonResponse.split("\"humidity\":")[1].split(",")[0];
                        String windSpeed = jsonResponse.split("\"speed\":")[1].split(",")[0];
                        String description = jsonResponse.split("\"description\":\"")[1].split("\"")[0];

                        // Update UI with weather information
                        SwingUtilities.invokeLater(() -> {
                            String message = "<html>City: " + city + "<br>Description: " + description + "<br>Temperature: " + temperature + "°C<br>" +
                                    "Humidity: " + humidity + "%<br>Wind Speed: " + windSpeed + " km/h</html>";
                            loadingLabel.setIcon(null);  // Remove loading icon
                            loadingLabel.setText(message);
                        });

                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            loadingLabel.setIcon(null);
                            JOptionPane.showMessageDialog(frame, "An error occurred: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                    return null;
                }

                @Override
                protected void done() {
                    // Reset the button to allow another fetch
                    fetchButton.setEnabled(true);
                }
            };

            // Disable button while loading
            fetchButton.setEnabled(false);
            // Start the background task
            worker.execute();
        });
    }
}
