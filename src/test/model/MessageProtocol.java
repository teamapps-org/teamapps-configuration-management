import org.teamapps.message.protocol.message.MessageDefinition;
import org.teamapps.message.protocol.message.MessageModelCollection;
import org.teamapps.message.protocol.model.EnumDefinition;
import org.teamapps.message.protocol.model.ModelCollection;
import org.teamapps.message.protocol.model.ModelCollectionProvider;

public class MessageProtocol implements ModelCollectionProvider {
	@Override
	public ModelCollection getModelCollection() {
		MessageModelCollection models = new MessageModelCollection("testConfigModel", "org.teamapps.config.test", 1);

		MessageDefinition testConfig = models.createModel("testConfig");
		MessageDefinition server = models.createModel("server");

		EnumDefinition nodeRole = models.createEnum("nodeRole", "ORIGIN", "EDGE", "BROKER");

		testConfig.addInteger("port", 1).setComment("comment for port").setDefaultValue("3");
		testConfig.addString("hostUrl",2).setComment("comment for hostUrl").setDefaultValue("https://the-url.com");
		testConfig.addMultiReference("peerNodes", server, 3).setComment("the peer nodes").setDefaultValue("a value?");


		server.addString("nodeId", 1).setComment("the node id").setDefaultValue("1234567890");
		server.addString("url", 2).setComment("the url").setDefaultValue("www.host.com");
		server.addInteger("port", 3).setComment("the port").setDefaultValue("8080");
		server.addEnum("role", nodeRole, 4).setComment("the role").setDefaultValue("EDGE");



		return models;
	}
}
