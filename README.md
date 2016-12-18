# Vetra

Auteurs : Léonard Benedetti et Émile-Hugo Spir

Déploiement : le fichier apk est disponible dans ce repository (Vetra.apk)

Vetra est une application permettant de garder le fil des séries que vous regardez, ainsi qu’en découvrir d'autres !
L’application utilise l'API 25, en l'absence de consignes contradictoires.

## Fonctionalités

### Top

En lançant l’application, vous êtes présenté avec un Top 20 des séries les plus populaires du moment. À tout moment, vous pouvez obtenir une version actualisée de cette liste en cliquant sur le bouton en haut à gauche de l'écran. Une notification vous sera envoyée lorsque le rafraichissement sera terminé, et la liste automatiquement mise à jour à l'aide d'un BroadcastReceiver. Tout problème vous sera signalé à l'aide d'un Toast. La liste est générée à l'aide du RecycleView.

<img src="https://www.taiki.fr/tmp/top.png" height="450">
<img src="https://www.taiki.fr/tmp/notification.png" height="450">

### Recherche

Vous pouvez aussi chercher des séries dans la base de données de TMDB à l’aide de notre fonctionnalitée de recherche. Les résultats sont mis à jour au fur et à mesure que vous tapez. Cette interface est présentée dans une deuxième activité.

<img src="https://www.taiki.fr/tmp/search.png" height="450">

### Vue détaillée

Cliquer sur une série, que ce soit dans la vue "Top" ou dans la recherche ouvre une vue détaillée de la série (une troisième activité).
Les images sont chargées à la volée et sont inséré dans la vue au fur et à mesure de leur téléchargement

<img src="https://www.taiki.fr/tmp/details_nofav.png" height="450">

Cette vue donne accès à quelques informations générales sur la série, le synopsis ainsi que les saisons sorties de la série avec poster, date de sortie et nombre d’épisode.
Cette vue permet aussi d’ajouter la série à vos favoris (à l'aide de l'icône en forme de coeur en haut) ainsi que d'ouvrir le site de la série dans chrome (à l'aide de l'autre icône dans la barre en haut).
Ajouter une série aux favoris change automatiquement l’icône des favoris

<img src="https://www.taiki.fr/tmp/details_fav.png" height="450">

Le logo de l’icône est évidemment automatiquement choisis en fonction de l'état de la série à l'ouverture.
Essayer de cliquer une deuxième fois sur l’icône propose de retirer la série de vos favoris.

<img src="https://www.taiki.fr/tmp/confirmation_de_suppression.png" height="450">

### Favoris

Une fois ajouté à vos favoris, la série apparait dans l’autre tab, visible dans la première activité.
Les séries présentes dans cette liste y resteront même si elles disparaissent du Top 20.

<img src="https://www.taiki.fr/tmp/favoris.png" height="450">

### Autres

L’application est intégralement traduite en français et en anglais. De plus, l'application obtiendra automatiquement la version traduite des informations sur la série.

<img src="https://www.taiki.fr/tmp/traduction.png" height="450">

L’application est aussi intégralement disponibles en deux gabarits: mode portrait et mode paysage.
Certaines activités sont même modifiés par un changement de perspective.

<img src="https://www.taiki.fr/tmp/gabarit2.png" height="450">

De plus, un easter egg est caché ! Si vous secouez votre téléphone dans la vue "Top", l’application s'en plaindra ;)

<img src="https://www.taiki.fr/tmp/easter_egg.png" height="450">

## Spécifications

[✓] des ressources pour afficher et traduire les chaînes de caractères: EN & FR, incluant ressources téléchargés<br>
[✓] des éléments graphiques de base (bouton, textview, image)<br>
[✓] deux gabarits différents pour une des activités: support des deux gabarits dans toutes nos activités<br>
[✓] au moins deux activités: 3 activités<br>
[✓] des notifications: un toast, une notification dans la barre de notification, une boite dialogue: Toast en cas d’erreur, notification à la fin de la mise à jour du top, boîte de dialogue pour easter-egg & suppression de favoris<br>
[✓] l’implémentation d’au moins un bouton dans l’action bar: un bouton dans l’activité principale, deux dans la vue détaillée<br>
[✓] un service de téléchargement: téléchargements des données et images de TMDB<br>
[✓] la notification de fin de téléchargement reçu dans un BroadCastReceiver: Mise à jour de la liste ou des images<br>
[✓] le traitement des données téléchargées: JSON<br>
[✓] un appel vers une application externe: Chrome<br>
[✓] l’affichage des données téléchargées dans une liste: 3 RecycleView à travers l’application<br>

## Bonus

[✓] l’enregistrement de données dans une base SQLite: les données téléchargés sont placés dans un cache SQLite (⚠️: changer la langue ne le supprime pas)<br>
[✗] la sauvegarde de préférences utilisateurs: impossibilité de trouver un cas d’usage pratique<br>
[✓] la lecture d’un capteur: easter egg tirant partie de l’accéléromètre<br>
[✓] des onglets à base de Fragments (difficile): Vue Top & Favoris
