package com.github.reload;

import javax.inject.Inject;
import org.apache.log4j.PropertyConfigurator;
import com.github.reload.components.ComponentsContext;
import com.github.reload.components.ComponentsContext.ServiceIdentifier;
import com.github.reload.conf.Configuration;

/**
 * Represents the RELOAD overlay where the local node is connected to
 * 
 */
public class Overlay {

	public static final String LIB_COMPANY = "zeroDenial";
	public static final String LIB_VERSION = "java-reload/0.1";

	public static final byte RELOAD_PROTOCOL_VERSION = 0x0a;

	static {
		PropertyConfigurator.configure("log4j.properties");
	}

	@Inject
	Configuration conf;

	@Inject
	Bootstrap bootstrap;

	@Inject
	ComponentsContext ctx;

	Overlay() {
	}

	public <T> T getService(ServiceIdentifier<T> serviceId) {
		return ctx.getService(serviceId);
	}

	/**
	 * Disconnect from this overlay and release all the resources. This method
	 * returns when the overlay has been left. All subsequent requests to this
	 * instance will fail.
	 */
	public void disconnect() {
		ctx.stopComponents();
	}

	@Override
	public int hashCode() {
		return bootstrap.hashCode();
	}

	/**
	 * Two overlay instances are considered equals if the assigned connectors
	 * are
	 * equals
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Overlay other = (Overlay) obj;
		return bootstrap.equals(other.bootstrap);
	}

	@Override
	public String toString() {
		return "OverlayConnection [overlay=" + conf.get(Configuration.OVERLAY_NAME) + ", localId=" + bootstrap.getLocalNodeId() + "]";
	}
}