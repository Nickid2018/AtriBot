package io.github.nickid2018.atribot.plugins.mcping;

import com.google.common.net.HostAndPort;
import lombok.extern.slf4j.Slf4j;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.Optional;

@Slf4j
public class AddressResolver {

    public static InitialDirContext initialDirContext;

    static {
        try {
            Class.forName("com.sun.jndi.dns.DnsContextFactory");
            Hashtable<String, String> hashtable = new Hashtable<String, String>();
            hashtable.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            hashtable.put("java.naming.provider.url", "dns:");
            hashtable.put("com.sun.jndi.dns.timeout.retries", "1");
            initialDirContext = new InitialDirContext(hashtable);
        } catch (Throwable throwable) {
            log.error("Failed to initialize SRV redirect resolved, some servers might not work", throwable);
        }
    }

    public static InetSocketAddress resolveSRV(HostAndPort hostAndPort) {
        if (initialDirContext == null)
            return null;
        if (hostAndPort.getPort() != 25565)
            return null;
        try {
            Attributes attributes = initialDirContext.getAttributes(
                "_minecraft._tcp." + hostAndPort.getHost(),
                new String[]{"SRV"}
            );
            Attribute attribute = attributes.get("srv");
            if (attribute != null) {
                String[] stringArray = attribute.get().toString().split(" ", 4);
                return new InetSocketAddress(stringArray[3], Integer.parseInt(stringArray[2]));
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
