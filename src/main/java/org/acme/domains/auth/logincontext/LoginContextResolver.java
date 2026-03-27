package org.acme.domains.auth.logincontext;

import io.smallrye.mutiny.Uni;
import org.acme.domains.account.Account;
import org.acme.domains.auth.dto.LoginContextData;
import org.acme.domains.shared.enums.Role;

public interface LoginContextResolver {
    Role supports();
    Uni<LoginContextData> resolve(Account account);
}
