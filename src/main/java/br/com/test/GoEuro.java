package br.com.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;

public class GoEuro {

	private static final Logger LOGGER = Logger.getLogger(GoEuro.class.getName());

	private static final String FILE_EXTENSION = ".csv";
	private static final String JSON_LONGITUDE = "longitude";
	private static final String JSON_LATITUDE = "latitude";
	private static final String JSON_GEO_POSITION = "geo_position";
	private static final String JSON_TYPE = "type";
	private static final String JSON_ID = "_id";
	private static final String ENDPOINT = "http://api.goeuro.com/api/v2/position/suggest/en/";

	private static Gson gson = new Gson();

	public static void main(String[] args) throws IOException {
		FileWriter fileWriter = null;
		BufferedWriter writer = null;

		try {
			String country = readParameters(0, args);
			String fileName = readParameters(1, args);

			if (country == null)
				throw new IllegalArgumentException("Location parameter missing.");

			File file = createFile(country, fileName);
			String response = getResponseFromWebservice(country);
			List<Map> jsonObject = gson.fromJson(response, List.class);

			fileWriter = new FileWriter(file);
			writer = new BufferedWriter(fileWriter);
			writeCSVFileFromString(jsonObject, writer);

		} catch (ArrayIndexOutOfBoundsException e) {
			LOGGER.log(Level.SEVERE, "Incorrect parameters.", e);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			writer.close();
			fileWriter.close();
		}
	}

	private static String readParameters(int position, String[] args) {
		try {
			return args[position];
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}

	private static void writeCSVFileFromString(List<Map> jsonObject,
			BufferedWriter writer) throws IOException {
		for (Map currentValue : jsonObject) {
			StringBuilder sb = new StringBuilder();
			if (currentValue.containsKey(JSON_ID)) {
				String value = String.valueOf((Double) currentValue
						.get(JSON_ID));
				sb.append(value.substring(0, value.indexOf(".")));
			}
			sb.append(",");

			if (currentValue.containsKey(JSON_TYPE))
				sb.append(currentValue.get(JSON_TYPE));
			sb.append(",");

			if (currentValue.containsKey(JSON_GEO_POSITION)) {
				Map geoPosition = (Map) currentValue.get(JSON_GEO_POSITION);
				if (geoPosition.containsKey(JSON_LATITUDE))
					sb.append(geoPosition.get(JSON_LATITUDE));
				sb.append(",");

				if (geoPosition.containsKey(JSON_LONGITUDE))
					sb.append(geoPosition.get(JSON_LONGITUDE));
			}
			writer.write(sb.toString() + "\n");
		}
	}

	private static File createFile(String country, String fileName)
			throws IOException {
		try {
			File file = new File(fileName != null ? fileName + FILE_EXTENSION
					: country + FILE_EXTENSION);

			if (!file.exists())
				file.createNewFile();
			return file;
		} catch (IOException e) {
			throw new IOException("Error creating a new file", e);
		}
	}

	private static String getResponseFromWebservice(String country)
			throws IOException {
		URL url;
		try {
			url = new URL(ENDPOINT + country);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");

			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(
					(conn.getInputStream())));

			StringBuffer buffer = new StringBuffer();
			String output;
			while ((output = br.readLine()) != null) {
				buffer.append(output);
			}
			conn.disconnect();

			String response = buffer.toString();
			return response;
		} catch (MalformedURLException e) {
			throw new MalformedURLException("Incorrect URL.");
		} catch (IOException e) {
			throw new IOException("Unable to get a response from the server.", e);
		}
	}

}
