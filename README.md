# 📁 Arka – Gestion familiale de fichiers et dossiers

**Arka** est une application **desktop multiplateforme** développée en **Kotlin** avec **JetBrains Compose**.  
Elle permet aux membres d’une famille de **gérer, organiser, stocker et partager leurs fichiers** à travers des **espaces personnels** ou **communs**, dans une interface intuitive et sécurisée.

Chaque utilisateur dispose :
- d’un espace personnel privé,
- d’un espace commun familial partagé,
- d’un système d’alertes, de rôles et de droits d’accès.

L’application intègre une **API serveur** via **Ktor** pour permettre la synchronisation avec des applications mobiles (ex : Arka Mobile).

---

## 🎯 Objectif du projet

Créer un **hub numérique familial**, sécurisé, personnalisable, et interopérable.  
Arka est conçu pour répondre aux besoins suivants :
- Centraliser les documents familiaux (administratif, scolarité, santé…)
- Proposer un espace personnel pour chaque utilisateur
- Favoriser la collaboration via des espaces partagés
- Permettre des rappels et alertes internes
- Servir de base serveur pour des extensions mobiles (scan, alertes, tâches)

---

## 🚀 Fonctionnalités principales

- ✅ Création de familles et ajout de membres
- 📂 Espaces **personnels** et **communs** pour les fichiers
- 🔐 Gestion des **droits d'accès** aux fichiers et dossiers
- 🗃️ Arborescence hiérarchique avec dossiers imbriqués
- 📎 Ajout de fichiers avec **métadonnées**
- 📅 Système d'**alertes personnalisées**
- 🧑‍🤝‍🧑 Gestion des rôles (admin, responsable, membre)
- 🔒 Authentification sécurisée avec **Bcrypt**
- 🌐 API REST intégrée avec **Ktor** pour liaison mobile

---

## 🧱 Stack technique

| Type | Outils |
|------|--------|
| **Langage principal** | Kotlin (JVM) |
| **Interface utilisateur** | JetBrains Compose for Desktop (Material 3 + Voyager) |
| **Base de données** | MySQL via [Ktorm](https://www.ktorm.org/) |
| **Backend/API** | Ktor |
| **Injection de dépendances** | Koin |
| **Sécurité** | Bcrypt (authentification) |
| **Picker de fichiers** | mpfilepicker |
| **Logs** | Kotlin Logging + Logback |
| **Temps/date** | kotlinx-datetime |

---

## ⚙️ Installation locale

### Prérequis

- Java 17+
- IntelliJ IDEA (version Community ou Ultimate)
- MySQL Server avec base de données configurée (voir `/resources/config/`)
- JetBrains Compose for Desktop (inclus dans Gradle)

---

## 📸 Aperçu

*à compléter avec des captures d’écran de l’interface (fenêtre principale, ajout fichier, alertes)*

---

## 📱 Extension mobile disponible

👉 Voir le projet lié : [Arka Mobile](https://github.com/jeanauryel/ArkaMobile)

---

## 🧑‍💻 Auteur

Jean-Auryel Akinotcho  
👨‍💻 [Portfolio](https://jeanauryel.github.io/portfolio) | [LinkedIn](https://www.linkedin.com/in/jean-auryel-isma%C3%ABl-akinotcho-a60a0661)

---

## ✅ Licence

Ce projet est proposé sous licence MIT.
