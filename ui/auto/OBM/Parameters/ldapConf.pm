package OBM::Parameters::ldapConf;

require Exporter;

use OBM::Parameters::common;

@ISA = qw(Exporter);
@EXPORT_const = qw( $ldapAdminLogin $NODE $ROOT $DOMAINROOT $POSIXUSERS $POSIXGROUPS $SYSTEMUSERS $DOMAINHOSTS $SAMBADOMAIN $SAMBAFREEUNIXID $SAMBAUSERS $SAMBAGROUPS $SAMBAHOSTS $POSTFIXCONF $MAILSHARE $GROUPOFNAMES );
@EXPORT_struct = qw($attributeDef $ldapStruct);
@EXPORT = (@EXPORT_const, @EXPORT_struct);
@EXPORT_OK = qw();

#
# Necessaire pour le bon fonctionnement du package
$debug=1;


#
# delaration de variables
#
# Le login de l'administrateur LDAP
$ldapAdminLogin = "ldapadmin";


#
# Declaration des type de donnees LDAP
#
# Type de noeuds possibles
$NODE = "organizationalUnit";
$ROOT = "rootLdap";
$DOMAINROOT = "domainRootLdap";
#
# Type de donnees possibles
$POSIXUSERS = "posixUsers";
$POSIXGROUPS = "posixGroups";
$SYSTEMUSERS = "systemUsers";
$DOMAINHOSTS = "domainHost";
$SAMBADOMAIN = "sambaDomain";
$SAMBAFREEUNIXID = "sambaFreeUnixId";
$SAMBAUSERS = "sambaUsers";
$SAMBAGROUPS = "sambaGroups";
$SAMBAHOSTS = "sambaHosts";
$POSTFIXCONF = "postfixConf";
$MAILSHARE = "mailShare";
$GROUPOFNAMES = "groupOfNames";

#
# Déclaration des attributs des différents types
$attributeDef = {
    $ROOT => {
        structural => 1,
        is_branch => 1,
        dn_prefix => "dc",
        dn_value => "name",
        objectclass => [ "dcObject", "organization" ],
        init_struct => sub {
            my( $ldapStruct, $parentDn ) = @_;
            require OBM::Ldap::typeRoot;
            return OBM::Ldap::typeRoot::initStruct( $ldapStruct, $parentDn );
        },
        get_db_value => sub {
            my( $parentDn, $domainId ) = @_;
            require OBM::Ldap::typeRoot;
            return OBM::Ldap::typeRoot::getDbValues( $parentDn, $domainId );
        },
        create_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typeRoot;
            return OBM::Ldap::typeRoot::createLdapEntry( $entry, $ldapEntry );
        },
        update_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typeRoot;
            return OBM::Ldap::typeRoot::updateLdapEntry( $entry, $ldapEntry );
        }
    },

    $DOMAINROOT => {
        structural => 1,
        is_branch => 1,
        dn_prefix => "dc",
        dn_value => "domain_name",
        objectclass => [ "dcObject", "organization" ],
        init_struct => sub {
            my( $ldapStruct, $parentDn ) = @_;
            require OBM::Ldap::typeDomainRoot;
            return OBM::Ldap::typeDomainRoot::initStruct( $ldapStruct, $parentDn );
        },
        get_db_value => sub {
            my( $parentDn, $domainId ) = @_;
            require OBM::Ldap::typeDomainRoot;
            return OBM::Ldap::typeDomainRoot::getDbValues( $parentDn, $domainId );
        },
        create_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typeDomainRoot;
            return OBM::Ldap::typeDomainRoot::createLdapEntry( $entry, $ldapEntry );
        },
        update_ldap => sub { return 0; }
    },

    $NODE => {
        structural => 1,
        is_branch => 1,
        dn_prefix => "ou",
        dn_value => "name",
        objectclass => [ "organizationalUnit" ],
        get_db_value => sub {
            my( $parentDn, $domainId ) = @_;
            require OBM::Ldap::typeNode;
            return OBM::Ldap::typeNode::getDbValues( $parentDn, $domainId );
        },
        create_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typeNode;
            return OBM::Ldap::typeNode::createLdapEntry( $entry, $ldapEntry );
        },
        update_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typeNode;
            return OBM::Ldap::typeNode::updateLdapEntry( $entry, $ldapEntry );
        }
    },

    $POSIXUSERS => {
        structural => 1,
        is_branch => 0,
        dn_prefix => "uid",
        dn_value => "user_login",
        objectclass => [ "posixAccount", "shadowAccount", "inetOrgPerson", "obmUser" ],
        get_db_value => sub {
            my( $parentDn, $domainId ) = @_;
            require OBM::Ldap::typePosixUsers;
            return OBM::Ldap::typePosixUsers::getDbValues( $parentDn, $domainId );
        },
        create_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typePosixUsers;
            return OBM::Ldap::typePosixUsers::createLdapEntry( $entry, $ldapEntry );
        },
        update_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typePosixUsers;
            return OBM::Ldap::typePosixUsers::updateLdapEntry( $entry, $ldapEntry );
        },
        update_passwd => sub {
            my( $ldapEntry, $passwdType, $newPasswd ) = @_;
            require OBM::Ldap::typePosixUsers;
            return OBM::Ldap::typePosixUsers::updatePasswd( $ldapEntry, $passwdType, $newPasswd );
        }
    },

    $SYSTEMUSERS => {
        structural => 1,
        is_branch => 0,
        dn_prefix => "uid",
        dn_value => "user_login",
        objectclass => [ "person", "posixAccount", "obmSystemUser" ],
        get_db_value => sub {
            my( $parentdn, $domainId ) = @_;
            require OBM::Ldap::typeSystemUsers;
            return OBM::Ldap::typeSystemUsers::getDbValues( $parentdn, $domainId );
        },
        create_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typeSystemUsers;
            return OBM::Ldap::typeSystemUsers::createLdapEntry( $entry, $ldapEntry );
        },
        update_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typeSystemUsers;
            return OBM::Ldap::typeSystemUsers::updateLdapEntry( $entry, $ldapEntry );
        }
    },

    $POSIXGROUPS => {
        structural => 1,
        is_branch => 0,
        dn_prefix => "cn",
        dn_value => "group_name",
        objectclass => [ "posixGroup", "obmGroup" ],
        get_db_value => sub {
            my( $parentdn, $domainId ) = @_;
            require OBM::Ldap::typePosixGroups;
            return OBM::Ldap::typePosixGroups::getDbValues( $parentdn, $domainId );
        },
        create_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typePosixGroups;
            return OBM::Ldap::typePosixGroups::createLdapEntry( $entry, $ldapEntry );
        },
        update_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typePosixGroups;
            return OBM::Ldap::typePosixGroups::updateLdapEntry( $entry, $ldapEntry );
        }
    },

    $MAILSHARE => {
        structural => 1,
        is_branch => 0,
        dn_prefix => "cn",
        dn_value => "mailshare_name",
        objectclass => [ "obmMailShare" ],
        get_db_value => sub {
            my( $parentdn, $domainId ) = @_;
            require OBM::Ldap::typeMailShare;
            return OBM::Ldap::typeMailShare::getDbValues( $parentdn, $domainId );
        },
        create_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typeMailShare;
            return OBM::Ldap::typeMailShare::createLdapEntry( $entry, $ldapEntry );
        },
        update_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typeMailShare;
            return OBM::Ldap::typeMailShare::updateLdapEntry( $entry, $ldapEntry );
        }
    },

    $SAMBADOMAIN => {
        structural => 1,
        is_branch => 0,
        dn_prefix => "sambaDomainName",
        dn_value => "samba_domain",
        objectclass => [ "sambaDomain", "obmSamba" ],
        get_db_value => sub {
            my( $parentdn, $domainId ) = @_;
            require OBM::Ldap::typeSambaDomain;
            return OBM::Ldap::typeSambaDomain::getDbValues( $parentdn, $domainId );
        },
        create_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typeSambaDomain;
            return OBM::Ldap::typeSambaDomain::createLdapEntry( $entry, $ldapEntry );
        },
        update_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typeSambaDomain;
            return OBM::Ldap::typeSambaDomain::updateLdapEntry( $entry, $ldapEntry );
        }
    },

    $SAMBAUSERS => {
        structural => 0,
        is_branch => 0,
        dn_prefix => "uid",
        dn_value => "user_login",
        objectclass => [ "sambaSamAccount" ],
        get_db_value => sub {
            my( $parentdn, $domainId ) = @_;
            require OBM::Ldap::typeSambaUsers;
            return OBM::Ldap::typeSambaUsers::getDbValues( $parentdn, $domainId );
        },
        create_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typeSambaUsers;
            return OBM::Ldap::typeSambaUsers::createLdapEntry( $entry, $ldapEntry );
        },
        update_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typeSambaUsers;
            return OBM::Ldap::typeSambaUsers::updateLdapEntry( $entry, $ldapEntry );
        },
        update_passwd => sub {
            my( $ldapEntry, $passwdType, $newPasswd ) = @_;
            require OBM::Ldap::typeSambaUsers;
            return OBM::Ldap::typeSambaUsers::updatePasswd( $ldapEntry, $passwdType, $newPasswd );
        }
    },

    $SAMBAGROUPS => {
        structural => 0,
        is_branch => 0,
        dn_prefix => "cn",
        dn_value => "group_name",
        objectclass => [ "sambaGroupMapping" ],
        get_db_value => sub {
            my( $parentdn, $domainId ) = @_;
            require OBM::Ldap::typeSambaGroups;
            return OBM::Ldap::typeSambaGroups::getDbValues( $parentdn, $domainId );
        },
        create_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typeSambaGroups;
            return OBM::Ldap::typeSambaGroups::createLdapEntry( $entry, $ldapEntry );
        },
        update_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typeSambaGroups;
            return OBM::Ldap::typeSambaGroups::updateLdapEntry( $entry, $ldapEntry );
        }
    },

    $SAMBAHOSTS => {
        structural => 1,
        is_branch => 0,
        dn_prefix => "uid",
        dn_value => "host_login",
        objectclass => [ "person", "sambaSamAccount", "obmSamba" ],
        get_db_value => sub {
            my( $parentdn, $domainId ) = @_;
            require OBM::Ldap::typeSambaHosts;
            return OBM::Ldap::typeSambaHosts::getDbValues( $parentdn, $domainId );
        },
        create_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typeSambaHosts;
            return OBM::Ldap::typeSambaHosts::createLdapEntry( $entry, $ldapEntry );
        },
        update_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typeSambaHosts;
            return OBM::Ldap::typeSambaHosts::updateLdapEntry( $entry, $ldapEntry );
        }
    },

    $POSTFIXCONF => {
        structural => 1,
        is_branch => 0,
        dn_prefix => "cn",
        dn_value => "postfixconf_name",
        objectclass => [ "obmPostfix" ],
        get_db_value => sub {
            my( $parentdn, $domainId ) = @_;
            require OBM::Ldap::typePostfixConf;
            return OBM::Ldap::typePostfixConf::getDbValues( $parentdn, $domainId );
        },
        create_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typePostfixConf;
            return OBM::Ldap::typePostfixConf::createLdapEntry( $entry, $ldapEntry );
        },
        update_ldap => sub {
            my( $entry, $ldapEntry ) = @_;
            require OBM::Ldap::typePostfixConf;
            return OBM::Ldap::typePostfixConf::updateLdapEntry( $entry, $ldapEntry );
        }
    }
};


#
# Déclaration de la structure

#
# Déclaration de la racine de l'annuaire
$ldapStruct = {
    dn => "",
    name => "local",
    node_type => "$ROOT",
    description => "Racine de l'annuaire",
    data_type => [],
    template => [],
    branch => [],
    ldap_server => {
        server => "",
        login => "",
        passwd => "",
        conn => ""
    }
};

#
# Déclaration de la branche des utilisateurs systèmes globaux
$ldapStruct->{"branch"}->[0] = {
    dn => "",
    name => "sysusers",
    node_type => "$NODE",
    description => "System users",
    data_type => [ $SYSTEMUSERS ],
    template => [],
    branch => []
};


# Déclaration de la branche des racines des domaines.

# Ce type de branche sera copier autant de fois qu'il y a de domaines à traiter.
# Chaque branche contient les informations d'un domaine.
# Le noeud de type DOMAINROOT peut contenir des informations d'authentifications
# spécifiques à un serveur LDAP (cf. noeud type ROOT).
$ldapStruct->{"template"}->[0] = {
    dn => "",
    name => "",
    node_type => "$DOMAINROOT",
    description => "Racine du domaine",
    data_type => [],
    template => [],
    branch => []
};

if( $obmModules->{"samba"} ) {
    # Si module Samba actif, on ajoute le type à cette branche
    push( @{$ldapStruct->{"template"}->[0]->{"data_type"}}, $SAMBADOMAIN );
}

# Banche contenant la déclaration des utilisateurs
my $usersDesc = {
    dn => "",
    name => "users",
    node_type => "$NODE",
    description => "Users account",
    data_type => [ $POSIXUSERS ],
    template => [],
    branch => []
};

if( $obmModules->{"samba"} ) {
    # Si module Samba actif, on ajoute le type à cette branche
    push( @{$usersDesc->{"data_type"}}, $SAMBAUSERS );
}

# Ajout dans la structure du type utilisateur
push( @{$ldapStruct->{"template"}->[0]->{"branch"}}, $usersDesc );

# Banche contenant la déclaration des utilisateurs systèmes
my $systemUsersDesc = {
    dn => "",
    name => "sysusers",
    node_type => "$NODE",
    description => "System users",
    data_type => [ $SYSTEMUSERS ],
    template => [],
    branch => []
};

# Ajout dans la structure du type utilisateur système
push( @{$ldapStruct->{"template"}->[0]->{"branch"}}, $systemUsersDesc );

# Banche contenant la déclaration des groupes
my $groupsDesc = {
    dn => "",
    name => "groups",
    node_type => "$NODE",
    description => "System Groups",
    data_type => [ $POSIXGROUPS ],
    template => [],
    branch => []
};

if( $obmModules->{"samba"} ) {
    # Si module Samba actif, on ajoute le type à cette branche
    push( @{$groupsDesc->{"data_type"}}, $SAMBAGROUPS );
}

# Ajout dans la structure du type groupe
push( @{$ldapStruct->{"template"}->[0]->{"branch"}}, $groupsDesc );

if( $obmModules->{"mail"} ) {
    # Banche contenant la déclaration des répertoires partagés
    my $mailShareDesc = {
        dn => "",
        name => "mailShare",
        node_type => "$NODE",
        description => "Share Directory",
        data_type => [ $MAILSHARE ],
        template => [],
        branch => []
    };

    # Ajout dans la structure du type boîte à lettres partagées
    push( @{$ldapStruct->{"template"}->[0]->{"branch"}}, $mailShareDesc );
}

if( $obmModules->{"samba"} ) {
    # Banche contenant la déclaration des hôtes du domaine windows
    my $hostDesc = {
        dn => "",
        name => "hosts",
        node_type => "$NODE",
        description => "Samba hosts",
        data_type => [ $SAMBAHOSTS ],
        template => [],
        branch => []
    };

    # Ajout dans la structure du type hôte
    push( @{$ldapStruct->{"template"}->[0]->{"branch"}}, $hostDesc );
}

if( $obmModules->{"mail"} ) {
    #
    # Banche contenant la déclaration de la configuration des services
    my $postConf = {
        dn => "",
        name => "servicesConfiguration",
        node_type => "$NODE",
        description => "Services configuration",
        data_type => [ $POSTFIXCONF ],
        template => [],
        branch => []
    };

    # Ajout dans la structure du type configuration de postfix
    push( @{$ldapStruct->{"template"}->[0]->{"branch"}}, $postConf );
}

return 1;
