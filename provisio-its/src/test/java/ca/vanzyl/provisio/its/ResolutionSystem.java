package ca.vanzyl.provisio.its;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.internal.impl.DefaultLocalPathComposer;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

public class ResolutionSystem {

    private final File localRepository;
    private final RepositorySystem system;
    private final RepositorySystemSession session;
    private final List<RemoteRepository> remoteRepositories;

    public ResolutionSystem(File localRepository) {
        this.localRepository = localRepository;
        this.system = new RepositorySystemSupplier().get();
        this.session = repositorySystemSession();
        this.remoteRepositories = new ArrayList<>();
    }

    public ArtifactType getArtifactType(String typeId) {
        return session.getArtifactTypeRegistry().get(typeId);
    }

    public void remoteRepository(String remoteRepository) {
        remoteRepositories.add(remoteRepository(
                new Repository(remoteRepository.substring(0, remoteRepository.indexOf("//")), remoteRepository)));
    }

    private RemoteRepository remoteRepository(Repository r) {
        RemoteRepository.Builder builder = new RemoteRepository.Builder(r.getId(), "default", r.getUrl());
        if (r.getUsername() != null && r.getPassword() != null) {
            Authentication auth = new AuthenticationBuilder()
                    .addUsername(r.getUsername())
                    .addPassword(r.getPassword())
                    .build();
            builder.setAuthentication(auth);
        }
        return builder.build();
    }

    public List<RemoteRepository> remoteRepositories() {
        return remoteRepositories;
    }

    public RepositorySystem repositorySystem() {
        return system;
    }

    public RepositorySystemSession repositorySystemSession() {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(localRepository);
        // We are not concerned with checking the _remote.repositories files
        try {
            session.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory(new DefaultLocalPathComposer())
                    .newInstance(session, localRepo));
        } catch (NoLocalRepositoryManagerException e) {
            // This should never happen
        }
        // Don't follow remote repositories in POMs
        session.setIgnoreArtifactDescriptorRepositories(true);
        session.setTransferListener(new QuietTransferListener());
        session.setRepositoryListener(new QuietRepositoryListener());
        return session;
    }

    // We're no looking to watch any output here but if we do, in fact, need to watch anything
    // we can make simple changes to these no-op implementations
    public static class QuietRepositoryListener extends AbstractRepositoryListener {}

    public static class QuietTransferListener extends AbstractTransferListener {}
}
