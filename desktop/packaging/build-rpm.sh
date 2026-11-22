#!/usr/bin/env bash
# Собирает RPM из готового app-image (build/compose/binaries/main/app) со своим
# .desktop: нормальное имя в меню и валидная категория Education.
#
# Использование: packaging/build-rpm.sh <version> [release]
# Требует rpmbuild (на Fedora atomic — запускать под bwrap с рабочим /usr/lib/rpm).
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
VERSION=${1:?укажите версию, например 1.0.0}
RELEASE=${2:-1}

APP="$ROOT/build/compose/binaries/main/app/lintech28-diary"
OUT="$ROOT/build/compose/binaries/main/rpm"
TOP="$ROOT/build/rpmbuild-menu"
DESKTOP="$ROOT/packaging/lintech28-diary.desktop"

if [ ! -x "$APP/bin/lintech28-diary" ]; then
  echo "Нет app-image: $APP. Сначала ./gradlew createDistributable" >&2
  exit 1
fi

rm -rf "$TOP"
mkdir -p "$TOP"/{BUILD,BUILDROOT,RPMS,SOURCES,SPECS,SRPMS} "$OUT"
cp -a "$APP" "$TOP/SOURCES/app"
install -m 0644 "$DESKTOP" "$TOP/SOURCES/lintech28-diary.desktop"
rm -f "$OUT/lintech28-diary-$VERSION-$RELEASE.x86_64.rpm"

cat > "$TOP/SPECS/lintech28-diary.spec" <<SPEC
%global __os_install_post %{nil}
%global __spec_install_post %{nil}

Name:           lintech28-diary
Version:        $VERSION
Release:        $RELEASE
Summary:        Электронный дневник ЛинТех 28
License:        Proprietary
Vendor:         Linteh28
BuildArch:      x86_64
AutoReqProv:    no
Requires:       /bin/sh
Requires:       bzip2-libs fontconfig freetype glib2 glibc graphite2 harfbuzz
Requires:       libbrotli libgcc libglvnd libglvnd-glx libpng libstdc++
Requires:       libX11 libXau libxcb libXext libxml2 pcre2 zlib-ng-compat

%description
Нативное десктопное приложение «Электронный дневник ЛинТех 28»:
дневник, оценки, расписание, цели и события школы.

%install
mkdir -p %{buildroot}/usr/lib
cp -a %{_sourcedir}/app %{buildroot}/usr/lib/lintech28-diary
install -Dm644 %{_sourcedir}/lintech28-diary.desktop \
  %{buildroot}/usr/share/applications/lintech28-diary.desktop

%files
%defattr(-,root,root,-)
/usr/lib/lintech28-diary
/usr/share/applications/lintech28-diary.desktop

%post
update-desktop-database -q /usr/share/applications 2>/dev/null || :

%postun
update-desktop-database -q /usr/share/applications 2>/dev/null || :
SPEC

rpmbuild -bb \
  --define "_topdir $TOP" \
  --define "_rpmdir $OUT" \
  --define "_rpmfilename lintech28-diary-$VERSION-$RELEASE.x86_64.rpm" \
  "$TOP/SPECS/lintech28-diary.spec"

echo "Готово: $OUT/lintech28-diary-$VERSION-$RELEASE.x86_64.rpm"
