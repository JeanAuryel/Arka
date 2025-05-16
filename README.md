# 📁 Arka – Gestion familiale de fichiers et dossiers

**Arka** est une application **desktop multiplateforme** développée en **Kotlin** avec **JetBrains Compose**. Elle permet aux membres d’une famille de **gérer, stocker et partager des fichiers** au sein d’espaces personnels ou communs.  
Chaque utilisateur peut gérer ses propres fichiers, partager des ressources avec sa famille, et gérer les permissions d’accès avec flexibilité.

---

## 🚀 Fonctionnalités principales

- ✅ Création de familles et ajout de membres
- 📂 Espaces **personnels** et **communs**
- 🔐 Gestion des **droits d'accès** aux fichiers et dossiers
- 🗃️ Dossiers hiérarchiques (dossiers imbriqués)
- 📎 Ajout de fichiers avec métadonnées
- 📅 Système d'alertes personnalisables
- 🧑‍🤝‍🧑 Gestion des rôles : admin, responsable, membre
- 🔒 Authentification sécurisée (hashage des mots de passe)
- 🔁 API intégrée avec **Ktor** pour l’extension mobile

---

## 🧱 Technologies utilisées

- **Langage principal** : Kotlin (JVM)
- **UI** : JetBrains Compose for Desktop (Material 3 + Voyager)
- **Base de données** : MySQL via **Ktorm**
- **Backend/API** : Ktor
- **Injection de dépendances** : Koin
- **Cryptage** : Bcrypt
- **File picker** : mpfilepicker
- **Logging** : Kotlin Logging + Logback
- **Date/heure** : kotlinx-datetime

---

## ⚙️ Installation locale

### Prérequis

- Java 17+
- IntelliJ IDEA (version Community suffisante)
- MySQL Server

### Cloner le projet

```bash
git clone https://github.com/Jeanauryel/arka.git
cd arka
