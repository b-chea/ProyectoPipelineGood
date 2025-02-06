import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.concurrent.TimeUnit;

public class TestPrueba {

    @Test
    public void hacer_una_busqueda(){

        System.setProperty("webdriver.chrome.driver", "C:\\Users\\viamatica\\Documents\\Driver\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");
        WebDriver driver = new ChromeDriver();

        //Se define tiempo de espera
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        //Abrimos una URL
        driver.get("https://www.google.com/");
        driver.manage().window().maximize();

        //Encontrar e interactuar con elementos
        driver.findElement(By.xpath("//textarea[@id='APjFqb']")).sendKeys("Cantidad de paises en América");
        driver.findElement(By.xpath("//textarea[@id='APjFqb']")).sendKeys(Keys.ENTER);

        //driver.findElement(By.xpath("//h3[contains(text(),'Mapa Político de América: Países y Capitales - Web')]")).click();
        driver.quit();
    }
}
