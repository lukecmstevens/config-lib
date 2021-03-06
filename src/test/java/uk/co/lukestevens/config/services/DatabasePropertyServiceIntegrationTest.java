package uk.co.lukestevens.config.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.co.lukestevens.config.ApplicationProperties;
import uk.co.lukestevens.config.application.SimpleApplicationProperties;
import uk.co.lukestevens.config.models.DatabaseConfig;
import uk.co.lukestevens.config.services.DatabasePropertyService;
import uk.co.lukestevens.config.services.PropertyService;
import uk.co.lukestevens.testing.mocks.DateMocker;
import uk.co.lukestevens.testing.db.TestDatabase;
import uk.co.lukestevens.utils.Dates;

public class DatabasePropertyServiceIntegrationTest {
	
	// TODO Change these tests to specifically test the SQL from the service
	
	DatabaseConfig config;
	
	@BeforeEach
	public void setup() throws IOException, SQLException {
		TestDatabase db = new TestDatabase();
		db.executeFile("setup");
		db.executeFile("addConfigs");
	    
	    DateMocker.setCurrentDate(new Date());
	    ApplicationProperties applicationProperties = new SimpleApplicationProperties(null, "test", null);
	    PropertyService service = new DatabasePropertyService(db, applicationProperties);
		this.config = new DatabaseConfig(service);
	}
	
	@Test
	public void testGetExistingProperty() throws IOException {
		config.load();
		
		String string = config.getAsString("string.property");
		assertEquals("some string", string);
		
		boolean bool = config.getAsBoolean("boolean.property");
		assertTrue(bool);
		
		double doubl = config.getAsDouble("double.property");
		assertEquals(16.4, doubl);
		
		int integer = config.getAsInt("int.property");
		assertEquals(3, integer);
	}
	
	@Test
	public void testGetMissingProperty() throws IOException {
		config.load();
		
		Object value = config.get("missing.property");
		assertNull(value);
	}

	@Test
	public void testEntrySet() throws IOException {
		config.load();
		
		List<Entry<Object, Object>> entries = new ArrayList<>(config.entrySet());
		Collections.sort(entries, (e1, e2) -> e1.getKey().toString().compareTo(e2.getKey().toString()));
		assertEquals(4, entries.size());
		
		{
			Entry<Object, Object> entry = entries.get(0);
			assertEquals("boolean.property", entry.getKey());
			assertEquals("true", entry.getValue());
		}
		
		{
			Entry<Object, Object> entry = entries.get(1);
			assertEquals("double.property", entry.getKey());
			assertEquals("16.4", entry.getValue());
		}
		
		{
			Entry<Object, Object> entry = entries.get(2);
			assertEquals("int.property", entry.getKey());
			assertEquals("3", entry.getValue());
		}
		
		{
			Entry<Object, Object> entry = entries.get(3);
			assertEquals("string.property", entry.getKey());
			assertEquals("some string", entry.getValue());
		}
	}
	
	@Test
	public void testLoadExpiredProperty() throws IOException, SQLException {
		config.load();
		
		{
			Object string = config.get("string.property");
			assertEquals("some string", string);
		}
		
		// Update config
		TestDatabase db = new TestDatabase();
		db.executeFile("updateConfig");
		
		DateMocker.setCurrentDate(new Date(Dates.millis() + 100));
		
		Object updated = config.get("string.property");
		assertEquals("updated", updated);
		
		Object neu = config.get("new.property");
		assertEquals("newproperty", neu);
	}
	
	@Test
	public void testLoadExpiredEntryset() throws IOException {
		config.load();
		
		assertEquals(4, config.entrySet().size());
		DateMocker.setCurrentDate(new Date(Dates.millis() + 100));
		assertEquals(0, config.entrySet().size());
	}
}
