package OBM::Update::updateGlobal;


$VERSION = "1.0";

$debug = 1;

use 5.006_001;
require Exporter;
use strict;


require OBM::toolBox;
require OBM::imapd;
require OBM::dbUtils;
require OBM::Ldap::ldapEngine;
require OBM::Cyrus::cyrusEngine;
require OBM::Cyrus::sieveEngine;
require OBM::Cyrus::cyrusRemoteEngine;
require OBM::Entities::obmRoot;
require OBM::Entities::obmDomainRoot;
require OBM::Entities::obmNode;
require OBM::Entities::obmSystemUser;
require OBM::Entities::obmUser;
require OBM::Entities::obmHost;
require OBM::Entities::obmGroup;
require OBM::Entities::obmMailshare;
require OBM::Entities::obmMailServer;
require OBM::Entities::obmSambaDomain;
require OBM::Update::utils;
use OBM::Update::commonGlobalIncremental qw(_updateState _doRemoteConf _runEngines _doUser _doGroup _doMailShare _doHost _doSystemUser _doSambaDomain _doMailServer _deleteDbEntity _tableNamePrefix);
use OBM::Parameters::common;
use OBM::Parameters::ldapConf;


sub new {
    my $self = shift;
    my( $dbHandler, $parameters ) = @_;

    # Définition des attributs de l'objet
    my %updateAttr = (
        user => undef,
        user_name => undef,
        domain => undef,
        delegation => undef,
        global => undef,
        dbHandler => undef,
        domainList => undef,
        engine => undef
    );


    if( !defined($dbHandler) || !defined($parameters) ) {
        croak( "Usage: PACKAGE->new(DBHANDLER, PARAMLIST)" );
    }elsif( !exists($parameters->{"user"}) && !exists($parameters->{"domain"}) && !exists($parameters->{"delegation" }) ) {
        croak( "Usage: PARAMLIST: table de hachage avec les cles 'user', 'domain' et 'delegation'" );
    }

    # Initialisation de l'objet
    $updateAttr{"global"} = $parameters->{"global"};
    $updateAttr{"dbHandler"} = $dbHandler;

    # Identifiant utilisateur
    if( defined($parameters->{"user"}) ) {
        $updateAttr{"user"} = $parameters->{"user"};

        my $query = "SELECT userobm_login FROM UserObm WHERE userobm_id=".$updateAttr{"user"};
        my $queryResult;
        if( !defined(&OBM::dbUtils::execQuery( $query, $dbHandler, \$queryResult )) ) {
            &OBM::toolBox::write_log( '[Update::updateGlobal]: probleme lors de l\'execution d\'une requete SQL : '.$self->{'dbHandler'}->err, 'W' );
            return 0;
        }

	    ( $updateAttr{"user_name"} ) = $queryResult->fetchrow_array();
	    $queryResult->finish();
    }

    # Identifiant de délégation
    if( defined($parameters->{"delegation"}) ) {
        $updateAttr{"delegation"} = $parameters->{"delegation"};
    }

    # Identifiant de domaine
    if( defined($parameters->{"domain"}) ) {
        $updateAttr{"domain"} = $parameters->{"domain"};
    }else {
        croak( "Le parametre domaine doit etre precise" );
    }


    # Obtention des informations sur les domaines nécessaires
    $updateAttr{"domainList"} = &OBM::Update::utils::getDomains( $updateAttr{"dbHandler"}, $updateAttr{"domain"} );


    # Obtention des serveurs LDAP par domaines
    &OBM::Update::utils::getLdapServer( $updateAttr{"dbHandler"}, $updateAttr{"domainList"} );


    # Initialisation du moteur LDAP
    $updateAttr{"engine"}->{"ldapEngine"} = OBM::Ldap::ldapEngine->new( $updateAttr{"domainList"} );
    if( !$updateAttr{"engine"}->{"ldapEngine"}->init( 1 ) ) {
        delete( $updateAttr{"engine"}->{"ldapEngine"} );
    }

    # Paramétrage des serveurs IMAP par domaine
    &OBM::Update::utils::getCyrusServers( $updateAttr{"dbHandler"}, $updateAttr{"domainList"} );
    if( !&OBM::imapd::getAdminImapPasswd( $updateAttr{"dbHandler"}, $updateAttr{"domainList"} ) ) {
        return undef;
    }

    # Paramétrage des serveurs SMTP-in par domaine
    &OBM::Update::utils::getSmtpInServers( $updateAttr{"dbHandler"}, $updateAttr{"domainList"} );

    # Paramétrage des serveurs SMTP-out par domaine
    &OBM::Update::utils::getSmtpOutServers( $updateAttr{"dbHandler"}, $updateAttr{"domainList"} );

    # Initialisation du moteur Cyrus
    $updateAttr{"engine"}->{"cyrusEngine"} = OBM::Cyrus::cyrusEngine->new( $updateAttr{"domainList"} );
    if( !$updateAttr{"engine"}->{"cyrusEngine"}->init() ) {
        delete( $updateAttr{"engine"}->{"cyrusEngine"} );
    }

    # Initialisation du moteur Sieve
    $updateAttr{"engine"}->{"sieveEngine"} = OBM::Cyrus::sieveEngine->new( $updateAttr{"domainList"} );
    if( !$updateAttr{"engine"}->{"sieveEngine"}->init() ) {
        delete( $updateAttr{"engine"}->{"sieveEngine"} );
    }


    bless( \%updateAttr, $self );
}


sub destroy {
    my $self = shift;

    my $engines = $self->{"engine"};
    while( my( $engineType, $engine ) = each(%{$engines}) ) {
        if( defined($engine) ) {
            $engine->destroy();
        }
    }
}


sub dump {
    my $self = shift;
    my @desc;

    push( @desc, $self );

    require Data::Dumper;
    print Data::Dumper->Dump( \@desc );

    return 1;
}


sub update {
    my $self = shift;
    my $return = 1;

    # On traite les suppressions
    $return = $self->_doGlobalDelete();
    # On traite les mises à jour
    $return = $return & $self->_doGlobalUpdate();

    if( $return ) {
        $return = $self->_doRemoteConf();
    }

    if( $return ) {
        $return = $self->_updateState();
    }

    if( $return ) {
        # Suite à une exécution en mode global, on s'assure que les tables Updated
        $self->_cleanUpdateDbTable();
    }

    return $return;
}


sub _doGlobalUpdate {
    my $self = shift;
    my $queryResult;
    my $globalReturn = 1;
    my $updateDbReturn = 1;
    my $return;

    if( !defined($self->{"domain"}) || ($self->{"domain"} !~ /^\d+$/) ) {
        &OBM::toolBox::write_log( "[Update::updateGlobal]: pas de domaine indique pour la MAJ totale", "W" );
        return 0;
    }
    my $domainDesc = &OBM::Update::utils::findDomainbyId( $self->{"domainList"}, $self->{"domain"} );

    if( !defined($domainDesc) ) {
        &OBM::toolBox::write_log( "[Update::updateGlobal]: domaine d'identifiant '".$self->{"domain"}."' inexistant", "W" );
        return 0;
    }


    &OBM::toolBox::write_log( "[Update::updateGlobal]: MAJ totale pour le domaine '".$domainDesc->{"domain_label"}."'", "W" );

    # MAJ des informations de domaine
    $updateDbReturn = $self->_updateDbDomain();
    if( !$updateDbReturn ) {
        &OBM::toolBox::write_log( "[Update::updateGlobal]: probleme de mise a jour du domaine dans la BD", "W" );
        return 0;
    }


    # Uniquement pour le metadomaine
    if( $self->{"domain"} == 0 ) {
        # Traitement des entités de type 'utilisateur système'
        my $query = "SELECT usersystem_id FROM UserSystem";
        if( !defined(&OBM::dbUtils::execQuery( $query, $self->{'dbHandler'}, \$queryResult )) ) {
            &OBM::toolBox::write_log( '[Update::updateGlobal]: probleme lors de l\'execution d\'une requete SQL : '.$self->{'dbHandler'}->err, 'W' );
            return 0;
        }

        while( my( $systemUserId ) = $queryResult->fetchrow_array() ) {
            my $object = $self->_doSystemUser( 1, 0, $systemUserId );

            $return = $self->_runEngines( $object );
            if( $return ) {
                # La MAJ de l'entité s'est bien passée, on met a jour la BD de
                # travail
                $updateDbReturn = $object->updateDbEntity( $self->{"dbHandler"} );
                if( $object->isLinks() ) {
                    $updateDbReturn = !$updateDbReturn || $object->updateDbEntityLinks( $self->{"dbHandler"} );
                }

                if( !$updateDbReturn ) {
                    &OBM::toolBox::write_log( "[Update::updateGlobal]: probleme de mise a jour d'un utilisateur systeme dans la BD", "W" );
                    $globalReturn = 0;
                }
            }else {
                &OBM::toolBox::write_log( "[Update::updateGlobal]: probleme de mise a jour d'un utilisateur systeme", "W" );
                $globalReturn = 0;
            }
        }
    }


    # Pour tous les domaines
    # Traitement des informations du domaine Samba
    my $object = $self->_doSambaDomain( 1, 0 );
    $return = $self->_runEngines( $object );

    if( $return ) {
        # La MAJ de l'entité s'est bien passée, on met a jour la BD de travail
        $updateDbReturn = $object->updateDbEntity( $self->{"dbHandler"} );
        if( $object->isLinks() ) {
            $updateDbReturn = !$updateDbReturn || $object->updateDbEntityLinks( $self->{"dbHandler"} );
        }

        if( !$updateDbReturn ) {
            &OBM::toolBox::write_log( "[Update::updateGlobal]: probleme de mise a jour du domaine Samba dans la BD", "W" );
            $globalReturn = 0;
        }
    }else {
        &OBM::toolBox::write_log( "[Update::updateGlobal]: probleme de mise a jour du domaine Samba", "W" );
        $globalReturn = 0;
    }


    # Traitement des entités de type 'obmMailServer'
    $object = $self->_doMailServer( 1, 0 );
    $return = $self->_runEngines( $object );

    if( $return ) {
        # La MAJ de l'entité s'est bien passée, on met a jour la BD de travail
        $updateDbReturn = $object->updateDbEntity( $self->{"dbHandler"} );
        if( $object->isLinks() ) {
            $updateDbReturn = !$updateDbReturn || $object->updateDbEntityLinks( $self->{"dbHandler"} );
        }

        if( !$updateDbReturn ) {
            &OBM::toolBox::write_log( "[Update::updateGlobal]: probleme de mise a jour des serveurs de courriers dans la BD", "W" );
            $globalReturn = 0;
        }
    }else {
        &OBM::toolBox::write_log( "[Update::updateGlobal]: probleme de mise a jour des serveurs de courriers", "W" );
        $globalReturn = 0;
    }


    # Pour tous les domaines, sauf le metadomaine
    if( $self->{"domain"} != 0 ) {
        # Mise à jour des partitions Cyrus
        my $updateMailSrv = OBM::Cyrus::cyrusRemoteEngine->new( $self->{"domainList"} );
        $return = $updateMailSrv->init();
        if( $return ) {
            $return = $updateMailSrv->update( "add" );
            $updateMailSrv->destroy();
    
            if( $return ) {
                # Si tout s'est bien passé, il faut rétablir les connexions à Cyrus
                if( defined($self->{"engine"}->{"cyrusEngine"}) ) {
                    if( !$self->{"engine"}->{"cyrusEngine"}->init() ) {
                        delete( $self->{"engine"}->{"cyrusEngine"} );
                    }
                }
            }else {
                &OBM::toolBox::write_log( "[Update::updateGlobal]: probleme lors de la mise a jour des partitions Cyrus du domaine '".$self->{"domain"}."' - Operation annulee !", "W" );
                return 0;
            }
        }
    }


    # Traitement des entités de type 'hote'
    my $query = "SELECT host_id FROM Host WHERE host_domain_id=".$self->{"domain"};
    if( !defined(&OBM::dbUtils::execQuery( $query, $self->{"dbHandler"}, \$queryResult )) ) {
        &OBM::toolBox::write_log( '[Update::updateGlobal]: probleme lors de l\'execution d\'une requete SQL : '.$self->{'dbHandler'}->err, 'W' );
        return 0;
    }

    while( my $hostId = $queryResult->fetchrow_array() ) {
        my $object = $self->_doHost( 1, 0, $hostId );

        my $return = $self->_runEngines( $object );
        if( $return ) {
            # La MAJ de l'entité s'est bien passée, on met a jour la BD de travail
            $updateDbReturn = $object->updateDbEntity( $self->{"dbHandler"} );
            if( $object->isLinks() ) {
                $updateDbReturn = !$updateDbReturn || $object->updateDbEntityLinks( $self->{"dbHandler"} );
            }

            if( !$updateDbReturn ) {
                &OBM::toolBox::write_log( "[Update::updateGlobal]: probleme de mise a jour d'un hote dans la BD", "W" );
                $globalReturn = 0;
            }
        }else {
            &OBM::toolBox::write_log( "[Update::updateGlobal]: probleme de mise a jour d'un hote", "W" );
            $globalReturn = 0;
        }
    }


    # Traitement des entités de type 'utilisateur'
    $query = "SELECT userobm_id FROM UserObm WHERE userobm_domain_id=".$self->{"domain"};
    if( !defined(&OBM::dbUtils::execQuery( $query, $self->{"dbHandler"}, \$queryResult )) ) {
        &OBM::toolBox::write_log( '[Update::updateGlobal]: probleme lors de l\'execution d\'une requete SQL : '.$self->{'dbHandler'}->err, 'W' );
        return 0;
    }

    while( my( $userId ) = $queryResult->fetchrow_array() ) {
        $object = $self->_doUser( 1, 0, $userId );

        my $return = $self->_runEngines( $object );
        if( $return ) {
            # La MAJ de l'entité s'est bien passée, on met a jour la BD de
            # travail
            $updateDbReturn = $object->updateDbEntity( $self->{"dbHandler"} );
            if( $object->isLinks() ) {
                $updateDbReturn = !$updateDbReturn || $object->updateDbEntityLinks( $self->{"dbHandler"} );
            }

            if( !$updateDbReturn ) {
                &OBM::toolBox::write_log( "[Update::updateGlobal]: probleme de mise a jour d'un utilisateur dans la BD", "W" );
                $globalReturn = 0;
            }
        }else {
            &OBM::toolBox::write_log( "[Update::updateGlobal]: probleme de mise a jour d'un utilisateur", "W" );
            $globalReturn = 0;
        }
    }


    # Traitement des entités de type 'groupe'
    $query = "SELECT group_id FROM UGroup WHERE group_privacy=0 AND group_domain_id=".$self->{"domain"};
    if( !defined(&OBM::dbUtils::execQuery( $query, $self->{'dbHandler'}, \$queryResult )) ) {
        &OBM::toolBox::write_log( '[Update::updateGlobal]: probleme lors de l\'execution d\'une requete SQL : '.$self->{'dbHandler'}->err, 'W' );
        return 0;
    }

    while( my( $groupId ) = $queryResult->fetchrow_array() ) {
        $object = $self->_doGroup( 1, 0, $groupId );

        my $return = $self->_runEngines( $object );
        if( $return ) {
            # La MAJ de l'entité s'est bien passée, on met a jour la BD de
            # travail
            $updateDbReturn = $object->updateDbEntity( $self->{"dbHandler"} );
            if( $object->isLinks() ) {
                $updateDbReturn = !$updateDbReturn || $object->updateDbEntityLinks( $self->{"dbHandler"} );
            }

            if( !$updateDbReturn ) {
                &OBM::toolBox::write_log( "[Update::updateGlobal]: probleme de mise a jour d'un groupe dans la BD", "W" );
                $globalReturn = 0;
            }
        }else {
            &OBM::toolBox::write_log( "[Update::updateGlobal]: probleme de mise a jour d'un groupe", "W" );
            $globalReturn = 0;
        }
    }


    # Traitement des entités de type 'mailshare'
    $query = "SELECT mailshare_id FROM MailShare WHERE mailshare_domain_id=".$self->{"domain"};
    if( !defined(&OBM::dbUtils::execQuery( $query, $self->{'dbHandler'}, \$queryResult )) ) {
        &OBM::toolBox::write_log( '[Update::updateGlobal]: probleme lors de l\'execution d\'une requete SQL : '.$self->{'dbHandler'}->err, 'W' );
        return 0;
    }

    while( my( $mailshareId ) = $queryResult->fetchrow_array() ) {
        $object = $self->_doMailShare( 1, 0, $mailshareId );

        my $return = $self->_runEngines( $object );
        if( $return ) {
            # La MAJ de l'entité s'est bien passée, on met a jour la BD de
            # travail
            $updateDbReturn = $object->updateDbEntity( $self->{"dbHandler"} );
            if( $object->isLinks() ) {
                $updateDbReturn = !$updateDbReturn || $object->updateDbEntityLinks( $self->{"dbHandler"} );
            }

            if( !$updateDbReturn ) {
                &OBM::toolBox::write_log( "[Update::updateGlobal]: probleme de mise a jour d'un repertoire partage de messagerie dans la BD", "W" );
                $globalReturn = 0;
            }
        }else {
            &OBM::toolBox::write_log( "[Update::updateGlobal]: probleme de mise a jour d'un repertoire partage de messagerie", "W" );
            $globalReturn = 0;
        }
    }

    return $globalReturn;
}


sub _doGlobalDelete {
    my $self = shift;
    my $queryResult;
    my $globalReturn = 1;

    if( !defined($self->{"domain"}) || ($self->{"domain"} !~ /^\d+$/) ) {
        &OBM::toolBox::write_log( "[Update::updateGlobal]: pas de domaine indique pour la MAJ totale", "W" );
        return 0;
    }
    my $domainDesc = &OBM::Update::utils::findDomainbyId( $self->{"domainList"}, $self->{"domain"} );

    if( !defined($domainDesc) ) {
        &OBM::toolBox::write_log( "[Update::updateGlobal]: domaine d'identifiant '".$self->{"domain"}."' inexistant", "W" );
        return 0;
    }


    &OBM::toolBox::write_log( "[Update::updateGlobal]: detection des suppressions en BD pour le domaine '".$domainDesc->{"domain_label"}."'", "W" );


    # Traitement des entités de type 'hote'
    my $query = "SELECT host_id FROM P_Host WHERE host_domain_id=".$self->{"domain"}." AND host_id NOT IN (SELECT host_id FROM Host WHERE host_domain_id=".$self->{"domain"}.")";
    if( !defined(&OBM::dbUtils::execQuery( $query, $self->{'dbHandler'}, \$queryResult )) ) {
        &OBM::toolBox::write_log( '[Update::updateGlobal]: probleme lors de l\'execution d\'une requete SQL : '.$self->{'dbHandler'}->err, 'W' );
        return 0;
    }

    while( my $hostId = $queryResult->fetchrow_array() ) {
        my $object = $self->_doHost( 1, 1, $hostId );

        my $return = $self->_runEngines( $object );
        $globalReturn = ($return && $self->_deleteDbEntity( "Host", $hostId )) & $globalReturn;
    }


    # Traitement des entités de type 'utilisateur'
    $query = "SELECT userobm_id FROM P_UserObm WHERE userobm_domain_id=".$self->{"domain"}." AND userobm_id NOT IN (SELECT userobm_id FROM UserObm WHERE userobm_domain_id=".$self->{"domain"}.")";
    if( !defined(&OBM::dbUtils::execQuery( $query, $self->{'dbHandler'}, \$queryResult )) ) {
        &OBM::toolBox::write_log( '[Update::updateGlobal]: probleme lors de l\'execution d\'une requete SQL : '.$self->{'dbHandler'}->err, 'W' );
        return 0;
    }

    while( my( $userId ) = $queryResult->fetchrow_array() ) {
        my $object = $self->_doUser( 1, 1, $userId );

        my $return = $self->_runEngines( $object );
        $globalReturn = ($return && $self->_deleteDbEntity( "UserObm", $userId)) & $globalReturn;
    }


    # Traitement des entités de type 'groupe'
    $query = "SELECT group_id FROM P_UGroup WHERE group_domain_id=".$self->{"domain"}." AND group_privacy=0 AND group_id NOT IN (SELECT group_id FROM UGroup WHERE group_domain_id=".$self->{"domain"}." AND group_privacy=0)";
    if( !defined(&OBM::dbUtils::execQuery( $query, $self->{'dbHandler'}, \$queryResult )) ) {
        &OBM::toolBox::write_log( '[Update::updateGlobal]: probleme lors de l\'execution d\'une requete SQL : '.$self->{'dbHandler'}->err, 'W' );
        return 0;
    }

    while( my( $groupId ) = $queryResult->fetchrow_array() ) {
        my $object = $self->_doGroup( 1, 1, $groupId );

        my $return = $self->_runEngines( $object );
        $globalReturn = ($return && $self->_deleteDbEntity( "UGroup", $groupId)) & $globalReturn;
    }


    # Traitement des entités de type 'mailshare'
    $query = "SELECT mailshare_id FROM P_MailShare WHERE mailshare_domain_id=".$self->{"domain"}." AND mailshare_id NOT IN (SELECT mailshare_id FROM MailShare WHERE mailshare_domain_id=".$self->{"domain"}.")";
    if( !defined(&OBM::dbUtils::execQuery( $query, $self->{'dbHandler'}, \$queryResult )) ) {
        &OBM::toolBox::write_log( '[Update::updateGlobal]: probleme lors de l\'execution d\'une requete SQL : '.$self->{'dbHandler'}->err, 'W' );
        return 0;
    }

    while( my( $mailshareId ) = $queryResult->fetchrow_array() ) {
        my $object = $self->_doMailShare( 1, 1, $mailshareId );

        my $return = $self->_runEngines( $object );
        $globalReturn = ($return && $self->_deleteDbEntity( "MailShare", $mailshareId )) & $globalReturn;
    }


    return $globalReturn;
}


sub _updateDbDomain {
    my $self = shift;

    if( !defined($self->{'dbHandler'}) ) {
        return 0;
    }
    my $dbHandler = $self->{'dbHandler'};

    if( !defined($self->{'domain'}) || ($self->{'domain'} !~ /^\d+$/) ) {
        &OBM::toolBox::write_log( '[Update::updateGlobal]: pas de domaine indique pour la MAJ totale', 'W' );
        return 0;
    }
    my $domainId = $self->{'domain'};


    # Les informations du domaine
    my $query = 'DELETE FROM P_Domain WHERE Domain_id='.$domainId;
    my $queryResult;
    if( !defined(&OBM::dbUtils::execQuery( $query, $dbHandler, \$queryResult )) ) {
        &OBM::toolBox::write_log( '[Update::updateGlobal]: probleme lors de l\'execution de la requete : '.$dbHandler->err, 'W' );
        return 0;
    }

    $query = 'INSERT INTO P_Domain SELECT * FROM Domain WHERE domain_id='.$domainId;
    if( !defined(&OBM::dbUtils::execQuery( $query, $dbHandler, \$queryResult )) ) {
        &OBM::toolBox::write_log( '[Update::updateGlobal]: probleme lors de l\'execution de la requete : '.$dbHandler->err, 'W' );
        return 0;
    }


    # Les hôtes serveurs de mails
    $query = 'DELETE FROM P_MailServer WHERE mailserver_host_id IN (SELECT host_id FROM P_Host WHERE host_domain_id='.$domainId.')';
    if( !defined(&OBM::dbUtils::execQuery( $query, $dbHandler, \$queryResult )) ) {
        &OBM::toolBox::write_log( '[Update::updateGlobal]: probleme lors de l\'execution de la requete : '.$dbHandler->err, 'W' );
        return 0;
    }

    $query = 'INSERT INTO P_MailServer SELECT * FROM MailServer WHERE mailserver_host_id IN (SELECT host_id FROM Host WHERE host_domain_id='.$domainId.')';
    if( !defined(&OBM::dbUtils::execQuery( $query, $dbHandler, \$queryResult )) ) {
        &OBM::toolBox::write_log( '[Update::updateGlobal]: probleme lors de l\'execution de la requete : '.$dbHandler->err, 'W' );
        return 0;
    }


    # Les informations associées aux hôtes serveurs de mails
    $query = 'DELETE FROM P_MailServerNetwork WHERE mailservernetwork_host_id IN (SELECT host_id FROM P_Host WHERE host_domain_id='.$domainId.')';
    if( !defined(&OBM::dbUtils::execQuery( $query, $dbHandler, \$queryResult )) ) {
        &OBM::toolBox::write_log( '[Update::updateGlobal]: probleme lors de l\'execution de la requete : '.$dbHandler->err, 'W' );
        return 0;
    }

    $query = 'INSERT INTO P_MailServerNetwork SELECT * FROM MailServerNetwork WHERE mailservernetwork_host_id IN (SELECT host_id FROM Host WHERE host_domain_id='.$domainId.')';
    if( !defined(&OBM::dbUtils::execQuery( $query, $dbHandler, \$queryResult )) ) {
        &OBM::toolBox::write_log( '[Update::updateGlobal]: probleme lors de l\'execution de la requete : '.$dbHandler->err, 'W' );
        return 0;
    }


    # Les informations du domaine Samba
    $query = 'DELETE FROM P_Samba WHERE samba_domain_id='.$domainId;
    if( !defined(&OBM::dbUtils::execQuery( $query, $dbHandler, \$queryResult )) ) {
        &OBM::toolBox::write_log( '[Update::updateGlobal]: probleme lors de l\'execution de la requete : '.$dbHandler->err, 'W' );
        return 0;
    }

    $query = 'INSERT INTO P_Samba SELECT * FROM Samba WHERE samba_domain_id='.$domainId;
    if( !defined(&OBM::dbUtils::execQuery( $query, $dbHandler, \$queryResult )) ) {
        &OBM::toolBox::write_log( '[Update::updateGlobal]: probleme lors de l\'execution de la requete : '.$dbHandler->err, 'W' );
        return 0;
    }


    return 1;
}


# Suite à une exécution en mode global, on s'assure que les tables Updated,
# Updatedinks et Deleted ne contiennent pas d'informations en rapport aux
# données conservées
sub _cleanUpdateDbTable {
    my $self = shift;
    my $queryResult;

    if( !defined($self->{"dbHandler"}) ) {
        return 0;
    }
    my $dbHandler = $self->{"dbHandler"};

    if( !defined($self->{domain}) ) {
        return 0;
    }


    # Purge de la table Updated
    my $query = "DELETE FROM Updated WHERE updated_domain_id=".$self->{domain};
    if( defined($self->{user}) ) {
        $query .= " AND updated_user_id=".$self->{user};
    }
    if( defined($self->{delegation}) ) {
        $query .= " AND updatedlinks_delegation=\"".$self->{delegation}."\"";
    }
    &OBM::toolBox::write_log( "[Update::updateGlobal]: purge de la table 'Updated'", "W" );
    if( !defined(&OBM::dbUtils::execQuery( $query, $dbHandler, \$queryResult )) ) {
        $query =~ s/\s+/ /g;

        &OBM::toolBox::write_log( '[Update::updateIncremental]: probleme lors de l\'execution de la requete \''.$query.'\'', 'W' );
        if( defined($dbHandler->err) ) {
           &OBM::toolBox::write_log( '[Update::updateGlobal]: '.$dbHandler->err, 'W' );
        }

        return 0;
    }


    # Purge de la table Updatedlinks
    $query = "DELETE FROM Updatedlinks WHERE updatedlinks_domain_id=".$self->{domain};
    if( defined($self->{user}) ) {
        $query .= " AND updatedlinks_user_id=".$self->{user};
    }
    if( defined($self->{delegation}) ) {
        $query .= " AND updatedlinks_delegation=\"".$self->{delegation}."\"";
    }
    &OBM::toolBox::write_log( "[Update::updateGlobal]: purge de la table 'Updatedlinks'", "W" );
    if( !defined(&OBM::dbUtils::execQuery( $query, $dbHandler, \$queryResult )) ) {
        $query =~ s/\s+/ /g;

        &OBM::toolBox::write_log( '[Update::updateIncremental]: probleme lors de l\'execution de la requete \''.$query.'\'', 'W' );
        if( defined($dbHandler->err) ) {
            &OBM::toolBox::write_log( '[Update::updateGlobal]: '.$dbHandler->err, 'W' );
        }

        return 0;
    }


    # Purge de la table Deleted
    $query = "DELETE FROM Deleted WHERE deleted_domain_id=".$self->{domain};
    if( defined($self->{user}) ) {
        $query .= " AND deleted_user_id=".$self->{user};
    }
    if( defined($self->{delegation}) ) {
        $query .= " AND deleted_delegation=\"".$self->{delegation}."\"";
    }
    &OBM::toolBox::write_log( "[Update::updateGlobal]: purge de la table 'Deleted'", "W" );
    if( !defined(&OBM::dbUtils::execQuery( $query, $dbHandler, \$queryResult )) ) {
        $query =~ s/\s+/ /g;

        &OBM::toolBox::write_log( '[Update::updateIncremental]: probleme lors de l\'execution de la requete \''.$query.'\'', 'W' );
        if( defined($dbHandler->err) ) {
            &OBM::toolBox::write_log( '[Update::updateGlobal]: '.$dbHandler->err, 'W' );
        }

        return 0;
    }


    return 1;
}
