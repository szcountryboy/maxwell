package com.zendesk.maxwell;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.github.shyiko.mysql.binlog.network.SSLMode;
import org.apache.commons.lang3.StringUtils;

import joptsimple.OptionSet;
import org.apache.http.client.utils.URIBuilder;

/**
 * Created by kaufmannkr on 12/23/15.
 */
public class MaxwellMysqlConfig {

	public String host;
	public Integer port;
	public String database;
	public String user;
	public String password;
	public SSLMode sslMode;
	public SSLMode replicationSslMode;
	public SSLMode schemaSslMode;
	public Map<String, String> jdbcOptions = new HashMap<>();
	public Integer connectTimeoutMS = 5000;

	public MaxwellMysqlConfig() {
		this.host = null;
		this.port = null;
		this.database = null;
		this.user = null;
		this.password = null;
		this.sslMode = null;
		this.replicationSslMode = null;
		this.schemaSslMode = null;

		this.jdbcOptions = new HashMap<>();
		this.jdbcOptions.put("zeroDateTimeBehavior", "convertToNull");
		this.jdbcOptions.put("connectTimeout", String.valueOf(connectTimeoutMS));
	}

	public MaxwellMysqlConfig(String host, Integer port, String database, String user, String password,
			SSLMode sslMode, SSLMode replicationSslMode, SSLMode schemaSslMode) {
		this.host = host;
		this.port = port;
		this.database = database;
		this.user = user;
		this.password = password;
		this.sslMode = sslMode;
		this.replicationSslMode = replicationSslMode;
		this.schemaSslMode = schemaSslMode;
	}

	public void useSSL(boolean should) {
		this.jdbcOptions.put("useSSL", String.valueOf(should));
	}

	public void requireSSL(boolean should) {
		this.jdbcOptions.put("requireSSL", String.valueOf(should));
	}

	public void verifyServerCertificate(boolean should) {
		this.jdbcOptions.put("verifyServerCertificate", String.valueOf(should));
	}

	public void setJDBCOptions(String opts) {
		if (opts == null)
			return;

		for ( String opt : opts.split("&") ) {
			String[] valueKeySplit = opt.trim().split("=");
			if (valueKeySplit.length == 2) {
				this.jdbcOptions.put(valueKeySplit[0], valueKeySplit[1]);
			}
		}
	}

	private void setSSLOptions() {
		// use the schema ssl mode and fallback to general ssl if undefined
		SSLMode jdbcSslMode = (schemaSslMode != null) ? schemaSslMode : sslMode;

		if (jdbcSslMode != null && jdbcSslMode != SSLMode.DISABLED) {
			this.useSSL(true); // for all SSL modes other than DISABLED, use SSL

			this.verifyServerCertificate(false); // default to not verify server cert
			this.requireSSL(false); // default to not require SSL

			if (jdbcSslMode == SSLMode.REQUIRED || jdbcSslMode == SSLMode.VERIFY_CA
					|| jdbcSslMode == SSLMode.VERIFY_IDENTITY) {
				this.requireSSL(true);
			}

			if (jdbcSslMode == SSLMode.VERIFY_IDENTITY) {
				this.verifyServerCertificate(true);
			}
		}
	}

	public String getConnectionURI(boolean includeDatabase) throws URISyntaxException {
		this.setSSLOptions();

		URIBuilder uriBuilder = new URIBuilder();

		uriBuilder.setScheme("jdbc:mysql");
		uriBuilder.setHost(host);
		uriBuilder.setPort(port);

		if (database != null && includeDatabase) {
			uriBuilder.setPath("/" + database);
		}

		for (Map.Entry<String, String> jdbcOption : jdbcOptions.entrySet()) {
			uriBuilder.addParameter(jdbcOption.getKey(), jdbcOption.getValue());
		}

		return uriBuilder.build().toString();
	}

	public String getConnectionURI() throws URISyntaxException { return getConnectionURI(true); }

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MaxwellMysqlConfig that = (MaxwellMysqlConfig) o;
		return Objects.equals(host, that.host) &&
				Objects.equals(port, that.port) &&
				Objects.equals(database, that.database) &&
				Objects.equals(user, that.user) &&
				Objects.equals(password, that.password) &&
				sslMode == that.sslMode &&
				replicationSslMode == that.replicationSslMode &&
				schemaSslMode == that.schemaSslMode &&
				Objects.equals(jdbcOptions, that.jdbcOptions) &&
				Objects.equals(connectTimeoutMS, that.connectTimeoutMS);
	}

	@Override
	public int hashCode() {
		return Objects
				.hash(host, port, database, user, password, sslMode, replicationSslMode, schemaSslMode,
						jdbcOptions, connectTimeoutMS);
	}
}
