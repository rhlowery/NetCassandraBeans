package com.ncb;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Session;
import java.beans.IntrospectionException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.Action;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.actions.OpenLocalExplorerAction;
import org.openide.awt.StatusDisplayer;
import org.openide.nodes.BeanNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.openide.util.actions.SystemAction;

class CassandraClusterFactory extends ChildFactory.Detachable<Cluster> implements Host.StateListener {

    private List<Cluster> clusters;
    private Session session;
    private ChangeListener listener;

    public CassandraClusterFactory() {
        this.clusters = new ArrayList<Cluster>();
    }

    @Override
    protected void addNotify() {
        PropertiesNotifier.addChangeListener(listener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ev) {
                clusters.add(connectToCluster());
                refresh(true);
            }
        });
    }

    @Override
    protected void removeNotify() {
        if (listener != null) {
            PropertiesNotifier.removeChangeListener(listener);
            listener = null;
        }
    }

    @Override
    protected boolean createKeys(List<Cluster> list) {
        list.addAll(clusters);
        return true;
    }

    @Override
    protected Node createNodeForKey(Cluster key) {
        ClusterNode node = null;
        try {
            node = new ClusterNode(key);
        } catch (IntrospectionException ex) {
            Exceptions.printStackTrace(ex);
        }
        return node;
    }

    private class ClusterNode extends BeanNode {
        @StaticResource
        private final String CASSANDRACLUSTERICON = "com/ncb/cassandra-cluster.png";
        public ClusterNode(Cluster key) throws IntrospectionException {
            super(key, Children.create(new CassandraContainerFactory(key, session), true));
            setDisplayName(key.getClusterName());
            setShortDescription("Clusters");
            setIconBaseWithExtension(CASSANDRACLUSTERICON);
        }
        @Override
        public Action getPreferredAction() {
            return null;
        }
        @Override
        public Action[] getActions(boolean context) {
            return new Action[]{SystemAction.get(OpenLocalExplorerAction.class)};
        }
    }

    private Cluster connectToCluster() {
        String cassandraCluster = NbPreferences.forModule(CassandraRootNode.class).get("cassandraCluster", "127.0.0.1:9042");
        String[] split = cassandraCluster.split(":");
        String host = split[0];
        String port = split[1];
        System.out.println("host = " + host);
        System.out.println("port = " + port);
        String clusterAuth = NbPreferences.forModule(CassandraRootNode.class).get(host+":"+port, null);
        Builder builder = Cluster.builder();
        builder.addContactPointsWithPorts(Collections.singleton(new InetSocketAddress(host, Integer.valueOf(port))));
        if ( clusterAuth != null ) {
            String[] split2 = clusterAuth.split(":");
            String user = split2[0];
            String pass = split2[1];
            builder.withCredentials(user, pass);
        }
        Cluster cluster = builder.build();
        cluster.register(this);
        session = cluster.connect();
        System.out.println("Connected to cluster " + cluster.getClusterName());
        return cluster;
    }

    @Override
    public void onAdd(Host host) {
        refresh(true);
        System.out.println("added: " + host);
        StatusDisplayer.getDefault().setStatusText("added: " + host);
    }

    @Override
    public void onUp(Host host) {
        refresh(true);
        System.out.println("upped: " + host);
        StatusDisplayer.getDefault().setStatusText("upped: " + host);
    }

    @Override
    public void onSuspected(Host host) {
        refresh(true);
        System.out.println("suspected: " + host);
        StatusDisplayer.getDefault().setStatusText("suspected: " + host);
    }

    @Override
    public void onDown(Host host) {
        refresh(true);
        System.out.println("downed: " + host);
        StatusDisplayer.getDefault().setStatusText("downed: " + host);
    }

    @Override
    public void onRemove(Host host) {
        refresh(true);
        System.out.println("removed: " + host);
        StatusDisplayer.getDefault().setStatusText("removed: " + host);
    }

}
