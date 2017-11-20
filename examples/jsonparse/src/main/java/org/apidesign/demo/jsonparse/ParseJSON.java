package org.apidesign.demo.jsonparse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import net.java.html.BrwsrCtx;
import net.java.html.json.Model;
import net.java.html.json.Models;
import net.java.html.json.Property;
import org.netbeans.html.context.spi.Contexts;
import org.netbeans.html.json.spi.Transfer;
import org.netbeans.html.wstyrus.TyrusContext;


@Model(className="RepositoryInfo", properties = {
    @Property(name = "id", type = int.class),
    @Property(name = "name", type = String.class),
    @Property(name = "owner", type = Owner.class),
    @Property(name = "private", type = boolean.class),
})
final class RepositoryCntrl {
    @Model(className = "Owner", properties = {
        @Property(name = "login", type = String.class)
    })
    static final class OwnerCntrl {
    }
}

public final class ParseJSON {
    public static void main(String... args) throws Exception {
        List<RepositoryInfo> repositories = new ArrayList<>();
        try (InputStream is = initializeStream(args)) {
            Models.parse(CONTEXT, RepositoryInfo.class, is, repositories);
        }
        
        System.err.println("there is " + repositories.size() + " repositories");
        repositories.stream().filter((repo) -> repo != null && repo.getOwner() != null).forEach((repo) -> {
            System.err.println("repository " + repo.getName() + " is owned by " + repo.getOwner().getLogin());
        });
    }

    private static InputStream initializeStream(String[] args) throws IOException {
        URL url;
        if (args.length == 0) {
            try {
                url = new URL("https://api.github.com/users/jersey/repos");
            } catch (Error t) {
                System.err.println("Unsupported https scheme (" + t.getMessage() + "), Try parsing from stdin:");
                System.err.println("$ curl https://api.github.com/users/jersey/repos | ./target/jsonparse -");
                System.exit(1);
                throw t;
            }
        } else {
            if ("-".equals(args[0])) {
                return System.in;
            } else {
                url = new URL(args[0]);
            }
        }
        return url.openStream();
    }

    private static final BrwsrCtx CONTEXT;
    static {
        CONTEXT = Contexts.newBuilder().
            register(Transfer.class, new TyrusContext(), 100).
            build();

        // initialize classes for SubstrateVM
        Models.fromRaw(CONTEXT, RepositoryInfo.class, null);
        Models.fromRaw(CONTEXT, Owner.class, null);
    }
}
