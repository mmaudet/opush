#!/bin/sh
###############################################################################
# OBM - File : install_obmdb_1.0.sh                                           #
#     - Desc : MySQL Database 1.0 installation script                         #
# 2005-06-08 ALIACOM                                                          #
###############################################################################
# $Id$
###############################################################################

# Mysql User, Password and Data lang var definition
U=obm
P="obm"
DB="obm"
DATA_LANG="en"

# We search for PHP interpreter (different name on Debian, RedHat, Mandrake)
PHP=`which php4 2> /dev/null`
if [ $? != 0 ]; then
  PHP=`which php 2> /dev/null`
  if [ $? != 0 ]; then
    PHP=`which php-cgi 2> /dev/null`
    if [ $? != 0 ]; then
      echo "Can't find php interpreter"
      exit
    fi
  fi
fi
echo $PHP : PHP interpreter found


# Database creation
echo "Database creation"
mysql -u $U -p$P < create_obmdb_0.9.mysql.sql

# Dictionnary data insertion
echo "Dictionnary data insertion"
mysql -u $U -p$P $DB < data-$DATA_LANG/obmdb_ref_0.9.sql

# Company Naf Code data insertion
echo "Company Naf Code data insertion"
mysql -u $U -p$P $DB < data-$DATA_LANG/obmdb_nafcode_0.9.sql

# Test data insertion
echo "Test data insertion"
mysql -u $U -p$P $DB < obmdb_test_values_0.9.sql

# Default preferences data insertion
echo "Default preferences data insertion"
mysql -u $U -p$P $DB < obmdb_default_values_0.9.sql

# Default preferences propagation on created users
echo "Default preferences propagation on created users"
$PHP ../../php/admin_pref/admin_pref_index.php -a user_pref_update

# Update calculated values
echo "Update calculated values"
$PHP ../../php/admin_data/admin_data_index.php -a data_update

# Update phonetics ans approximative searches
echo "Update phonetics and approximative searches"
$PHP ../../php/admin_data/admin_data_index.php -a sound_aka_update
