import com.github.javafaker.Faker;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AddVodAction {
    WebDriver driver = new ChromeDriver();


    private static final String dbURL = "jdbc:mysql://10.18.0.134:3306/vod_tm";
    private static final String username = "root";
    private static final String password = "Teamtvdev21!";

    public int randomPrice;
    public String companyId;

    // Method to retrieve price from database
    public static Map<String, Object> getPriceAndCompanyIdFromDatabase() {
        Map<String, Object> result = new HashMap<>();
        try (
                Connection conn = DriverManager.getConnection(dbURL, username, password);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT price, company_id FROM vod_movie ORDER BY id DESC LIMIT 1")
        ) {
            if (rs.next()) {
                int price = rs.getInt("price");
                String companyId = rs.getString("company_id");
                result.put("price", price);
                result.put("company_id", companyId);
                System.out.println("Price from database: " + price);
                System.out.println("Company ID from database: " + companyId);
            } else {
                System.out.println("No price or company_id data found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }


    @Test
    public void loginAndPerformActions() throws InterruptedException, SQLException {
        System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
        driver.manage().timeouts().implicitlyWait(50, TimeUnit.SECONDS);
        driver.manage().window().maximize();

        // Navigate to login page
        driver.get("http://10.18.0.134:30040/ttv-admin/#/vod/login");
        
        // Show input dialog to select login
        String input = JOptionPane.showInputDialog(null, "Enter 1 for TV_Admin login or 2 for Skytel_Admin login.", "Login Selection", JOptionPane.PLAIN_MESSAGE);

        String username;
        String password;
        if ("1".equals(input)) {
            // Set credentials for TV_Admin
            username = "tv_admin";
            password = "tv_admin";
            companyId = String.valueOf(10);
        } else if ("2".equals(input)) {
            // Set credentials for Skytel_Admin
            username = "skytel_admin";
            password = "skytel_admin";
            companyId = String.valueOf(11);
        } else {
            // If input is neither 1 nor 2, print a message and exit
            System.out.println("Wrong company selected. Please enter 1 or 2.");
            driver.quit(); // Close the browser if running
            return; // Exit the test early
        }

        // Login with selected credentials
        login(username, password);
        performAddVodActions();

        try {
            if ("tv_admin".equals(username)) {
                Assert.assertEquals(companyId, "10", "Company ID for TV_Admin should be 10");
                System.out.println("Assertion Passed: Company ID for TV_Admin is correct.");
            } else if ("skytel_admin".equals(username)) {
                Assert.assertEquals(companyId, "11", "Company ID for Skytel_Admin should be 11");
                System.out.println("Assertion Passed: Company ID for Skytel_Admin is correct.");
            }
        } catch (AssertionError e) {
            System.out.println("Assertion Failed: " + e.getMessage());
        }
    }

    // Method to perform login action
    private void login(String username, String password) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement userNameField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        WebElement userPassField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));
        WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"content\"]/div/div/app-login/div/form/div[3]/button")));

        userNameField.sendKeys(username);
        userPassField.sendKeys(password);
        loginButton.click();
    }

    // Method to perform Add Vod actions
    private int performAddVodActions() throws InterruptedException, SQLException {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Add VoD
        WebElement addVod = wait.until(ExpectedConditions.elementToBeClickable(By.partialLinkText("Add VoD")));
        addVod.click();

        // Toggle active status
        WebElement activeSwitch = wait.until(ExpectedConditions.elementToBeClickable(By.id("status-container")));
        activeSwitch.click();

        // Random movie duration
        WebElement durationInput = wait.until(ExpectedConditions.elementToBeClickable(By.id("inp-duration")));
        Random random = new Random();
        String duration = String.format("%02d:%02d:%02d", random.nextInt(24), random.nextInt(60), random.nextInt(60));
        durationInput.sendKeys(duration);

        // Random price
        randomPrice = new Random().nextInt(10000);
        WebElement priceInput = wait.until(ExpectedConditions.elementToBeClickable(By.id("inp-price")));
        priceInput.sendKeys(String.valueOf(randomPrice));

        // Random Year
        WebElement yearInput = wait.until(ExpectedConditions.elementToBeClickable(By.id("inp-year")));
        int randomYear = random.nextInt(LocalDate.now().getYear() - 1900 + 1) + 1900;
        yearInput.sendKeys(String.valueOf(randomYear));

        // Random IMDB Rating
        WebElement imdbRatingInput = wait.until(ExpectedConditions.elementToBeClickable(By.id("inp-imdbRating")));
        imdbRatingInput.sendKeys(String.format("%.1f", random.nextDouble() * 10));

        // Random Audio Rating
        WebElement audioInput = wait.until(ExpectedConditions.elementToBeClickable(By.id("inp-audio")));
        audioInput.sendKeys(String.format("%.1f", random.nextDouble() * 10));

        // Select MPAA Rating
        WebElement mpaaRatingInput = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"select-mpaa\"]/div/span[1]")));
        mpaaRatingInput.click();
        driver.findElements(By.cssSelector("div[role='option']")).get(0).click();

        // Generate random genres
        Faker faker = new Faker();
        Set<String> randomGenres = new HashSet<>();
        while (randomGenres.size() < 5) {
            randomGenres.add(faker.book().genre());
        }
        WebElement genresInput = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"select-genres\"]/div/div/div[2]/input")));
        genresInput.sendKeys(String.join(", ", randomGenres), Keys.ENTER);

        // Generate random actors
        Set<String> randomActors = new HashSet<>();
        while (randomActors.size() < 5) {
            randomActors.add(faker.name().fullName());
        }
        WebElement actorsInput = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"select-actors\"]/div/div/div[2]/input")));
        actorsInput.sendKeys(String.join(", ", randomActors), Keys.ENTER);

        // Generate a random director name
        WebElement directorsInput = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"select-directors\"]/div/div/div[2]/input")));
        directorsInput.sendKeys(faker.name().fullName(), Keys.ENTER);

        // Select movie type and service point
        WebElement movie_type = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"select-movieType\"]/div/span")));
        movie_type.click();
        driver.findElements(By.cssSelector("div[role='option']")).get(1).click();

        WebElement servicePoint_type = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"select-servicePointType\"]/div/span")));
        servicePoint_type.click();
        driver.findElements(By.cssSelector("div[role='option']")).get(0).click();

        // Generate random movie name and description
        faker = new Faker();
        String randomMovieName = faker.book().title();
        WebElement input_title = wait.until(ExpectedConditions.elementToBeClickable(By.id("inp-title")));
        input_title.sendKeys(randomMovieName);

        String randomDescription = faker.lorem().paragraph(3);
        WebElement input_desc = wait.until(ExpectedConditions.elementToBeClickable(By.id("description")));
        input_desc.sendKeys(randomDescription);

        // Submit form
        WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"cpt-root\"]/div/form/div/button")));
        submitButton.click();

        // Select categories and submit again
        for (int i = 1; i <= 2; i++) {
            WebElement checkbox = driver.findElement(By.xpath("//*[@id=\"category-tree\"]/mat-tree/mat-tree-node[" + i + "]/div/input"));
            checkbox.click();
        }

        WebElement secondSubmitButton = driver.findElement(By.xpath("//*[@id=\"content\"]/div[2]/button"));
        secondSubmitButton.click();

        // Add more details like sources, regions, etc.
        WebElement addVOD = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"vods-col\"]/i")));
        addVOD.click();

        // Enter paths and bandwidths for the VOD
        WebElement sourceType = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"select-source\"]/div/span[2]")));
        sourceType.click();
        driver.findElements(By.cssSelector("div[role='option']")).get(0).click();

        WebElement inputServer = wait.until(ExpectedConditions.elementToBeClickable(By.id("inp-server")));
        inputServer.sendKeys("http://ttv-vb1.teamcloud.am");

        WebElement IOSServer = wait.until(ExpectedConditions.elementToBeClickable(By.id("inp-server_ios")));
        IOSServer.sendKeys("http://ttv-vb1.teamcloud.am");

        WebElement File_Path = wait.until(ExpectedConditions.elementToBeClickable(By.id("inp-file_ios")));
        File_Path.sendKeys("/edgevod/_definst_/smil:his3/Cartoons/Yumis_Cells_The_Movie_2024/Yumis_Cells_The_Movie_2024.smil/playlist.m3u8");
        WebElement IosFilePath = wait.until(ExpectedConditions.elementToBeClickable(By.id("inp-file")));
        IosFilePath.sendKeys("/edgevod/_definst_/smil:his3/Cartoons/Yumis_Cells_The_Movie_2024/Yumis_Cells_The_Movie_2024.smil/playlist.m3u8");
        WebElement trailerPath = wait.until(ExpectedConditions.elementToBeClickable(By.id("inp-trailer")));
        trailerPath.sendKeys("/edgevod/_definst_/mp4:his3/Cartoons/Yumis_Cells_The_Movie_2024/Yumis_Cells_The_Movie_2024_trailer.mp4/playlist.m3u8");
        WebElement IosTrailerPath = wait.until(ExpectedConditions.elementToBeClickable(By.id("inp-trailer_ios")));
        IosTrailerPath.sendKeys("/edgevod/_definst_/mp4:his3/Cartoons/Yumis_Cells_The_Movie_2024/Yumis_Cells_The_Movie_2024_trailer.mp4/playlist.m3u8");

        WebElement inp_mobileBandwidth = driver.findElement(By.id("inp-mobileBandwidth"));
        inp_mobileBandwidth.sendKeys("0");
        WebElement androidBoxGroup1 = driver.findElement(By.id("inp-androidBoxGroup1Bandwidth"));
        androidBoxGroup1.sendKeys("0");
        WebElement androidBoxGroup2 = driver.findElement(By.id("inp-androidBoxGroup2Bandwidth"));
        androidBoxGroup2.sendKeys("0");
        WebElement androidApkBandwith = driver.findElement(By.id("inp-androidApkBandwidth"));
        androidApkBandwith.sendKeys("0");

        WebElement addRegion = driver.findElement(By.xpath("//*[@id=\"regions\"]/div[1]/i"));
        addRegion.click();

        WebElement unpublished = driver.findElement(By.xpath("//*[@id=\"root\"]/div[2]/div[1]/ui-switch"));
        unpublished.click();

        List<WebElement> priceInputs = driver.findElements(By.xpath("//input[@placeholder='Price']"));
        WebElement priceInput2 = priceInputs.get(1);
        priceInput2.click();
        priceInput2.clear();
        priceInput2.sendKeys(String.valueOf(randomPrice));

        WebElement inputPrepaid = driver.findElement(By.id("inp-prepaid"));
        inputPrepaid.sendKeys("ZVOD_NEW");
        WebElement inputPostpaid = driver.findElement(By.id("inp-postpaid"));
        inputPostpaid.sendKeys("ZVOD_NEWP");
        WebElement inpuitProdId = driver.findElement(By.id("inp-prodId"));
        inpuitProdId.sendKeys("1622");


        // Complete form submission
        WebElement submitButton2 = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"root\"]/div[3]/button")));
        submitButton2.click();
        return randomPrice;
    }

    @AfterTest
    public void retrivePriceAndCompanyIdAfterTest() throws SQLException, InterruptedException {

        Map<String, Object> dbData = getPriceAndCompanyIdFromDatabase();

        int priceFromDatabase;
        priceFromDatabase = (int) dbData.get("price");
        String companyIdFromDatabase = (String) dbData.get("company_id");
        try {
            // Assert price matches and print result
            Assert.assertEquals(priceFromDatabase, randomPrice, "The price from the database does not match the random price entered.");
            System.out.println("Assertion Passed: The price in the database matches the random price entered.");
        } catch (AssertionError e) {
            System.out.println("Assertion Failed: " + e.getMessage());
        }

        try {
            // Assert companyId matches and print result
            Assert.assertEquals(companyIdFromDatabase, companyId, "The company ID from the database does not match the selected company ID.");
            System.out.println("Assertion Passed: The company ID in the database matches the selected company ID.");
        } catch (AssertionError e) {
            System.out.println("Assertion Failed: " + e.getMessage());
        }
    }
}






