"""Recette Chrome optionnelle. Playwright installé séparément, application déjà lancée.

Sans option : connexion, captures et vérifications en lecture.
--importer : importe six films Ghibli (nécessite le réseau et un rôle programmateur/admin).
--reserver-et-annuler : crée puis annule uniquement sa propre réservation de recette.
Identifiants dans CINEMA_QA_USERNAME et CINEMA_QA_PASSWORD, jamais dans les captures.
"""
import os, json, sys, base64
from pathlib import Path
from playwright.sync_api import sync_playwright

base=os.environ.get('CINEMA_QA_URL', 'http://localhost:8080').rstrip('/')
sortie=Path('.tools/visuels'); sortie.mkdir(exist_ok=True)
def charger_images(page):
    page.locator('img[loading="lazy"]').evaluate_all('(images)=>images.forEach(i=>i.loading="eager")')
    page.wait_for_function('Array.from(document.images).every(i=>i.complete)',timeout=20000)
    page.evaluate('async()=>{await Promise.all(Array.from(document.images).map(i=>i.decode().catch(()=>{})));}')
    for image in page.locator('img').all():
        image.scroll_into_view_if_needed()
    page.evaluate('window.scrollTo(0,0)')
with sync_playwright() as p:
    navigateur=p.chromium.launch(channel='chrome',headless=True)
    contexte=navigateur.new_context(viewport={'width':1440,'height':1000},device_scale_factor=1)
    page=contexte.new_page()
    erreurs=[]
    page.on('pageerror',lambda e: erreurs.append(str(e)))
    page.goto(base+'/login')
    page.locator('#username').fill(os.environ['CINEMA_QA_USERNAME'])
    page.locator('#password').fill(os.environ['CINEMA_QA_PASSWORD'])
    page.get_by_role('button',name='Se connecter',exact=True).click()
    page.wait_for_load_state('networkidle')
    assert '/login' not in page.url, page.url
    if '--importer' in sys.argv:
        titres=['Le Voyage de Chihiro','Mon voisin Totoro','Le Château dans le ciel','Princesse Mononoké','Le Château ambulant','Kiki la petite sorcière']
        for titre in titres:
            page.goto(base+'/admin/catalogue',wait_until='domcontentloaded')
            carte=page.locator('.import-card').filter(has=page.get_by_role('heading',name=titre,exact=True))
            assert carte.count()==1, f'Film externe absent: {titre}'
            carte.get_by_role('button',name='Importer ce film').click(timeout=40000)
            page.wait_for_url('**/films/*',timeout=40000)
            print(json.dumps({'import':titre,'fiche':page.url},ensure_ascii=False),flush=True)
    resultats=[]
    for chemin,nom in [('/','accueil'),('/films','catalogue'),('/seances','seances'),('/admin/catalogue','import'),('/admin/films','gestion-films')]:
        response=page.goto(base+chemin,wait_until='networkidle')
        assert response.status==200,(chemin,response.status)
        charger_images(page)
        page.screenshot(path=str(sortie/(nom+'-bureau.png')),full_page=True)
        resultats.append({'page':chemin,'statut':response.status,'debordement':page.evaluate('document.documentElement.scrollWidth > innerWidth'),'images_invalides':page.locator('img').evaluate_all('(imgs)=>imgs.filter(i=>i.complete && !i.naturalWidth).map(i=>i.src)')})
    page.goto(base+'/seances')
    lien_fiche=page.locator('.screening-info a[href^="/films/"]').first
    fiche=lien_fiche.get_attribute('href') if lien_fiche.count() else '/films'
    premiere=page.locator('a[href^="/reservation/"]').first
    lien_reservation=premiere.get_attribute('href') if premiere.count() else '/login'
    if premiere.count():
        premiere.click()
        page.locator('#nombrePlaces').fill('2')
        estimation=page.locator('[data-estimation]').inner_text()
        assert '€' in estimation,estimation
        page.screenshot(path=str(sortie/'reservation-bureau.png'),full_page=True)
        resultats.append({'reservation':'formulaire','estimation_2_places':estimation})
        if '--reserver-et-annuler' in sys.argv:
            seance_id=int(page.url.rsplit('/',1)[1])
            def places():
                return next(s['placesRestantes'] for s in contexte.request.get(base+'/api/seances').json() if s['id']==seance_id)
            avant=places()
            page.locator('#emailClient').fill('recette-ui@example.test')
            page.get_by_role('button',name='Confirmer la réservation',exact=True).click()
            page.wait_for_url('**/reservation/confirmation')
            code=page.locator('code.code').inner_text()
            assert places()==avant-2
            page.screenshot(path=str(sortie/'confirmation-bureau.png'),full_page=True)
            auth=base64.b64encode((os.environ['CINEMA_QA_USERNAME']+':'+os.environ['CINEMA_QA_PASSWORD']).encode()).decode()
            annulation=contexte.request.delete(base+'/api/reservations/'+code,headers={'Authorization':'Basic '+auth})
            assert annulation.status==204
            assert places()==avant
            resultats.append({'reservation_reelle':{'places':2,'confirmation':True,'annulation':204,'places_restituees':True}})
    for largeur in (390,320):
        page.set_viewport_size({'width':largeur,'height':844})
        for chemin,nom in [('/','accueil'),('/films','catalogue'),('/seances','seances'),(fiche,'fiche-film'),(lien_reservation,'reservation'),('/admin/films','gestion-films')]:
            page.goto(base+chemin,wait_until='networkidle')
            charger_images(page)
            page.screenshot(path=str(sortie/(nom+f'-mobile-{largeur}.png')),full_page=True)
            resultats.append({'page':chemin,'largeur':largeur,'debordement':page.evaluate('document.documentElement.scrollWidth > innerWidth')})
        page.locator('[data-menu]').click()
        assert page.locator('#navigation').is_visible()
        page.keyboard.press('Escape')
        assert not page.locator('#navigation').is_visible()
    page.emulate_media(reduced_motion='reduce')
    page.goto(base)
    animation=page.locator('.hero-image').evaluate_all('(images)=>images.map(e=>getComputedStyle(e).animationDuration)')
    assert all(d=='0s' for d in animation),animation
    resultats.append({'reduction_mouvement':animation})
    public=navigateur.new_context(viewport={'width':1440,'height':1000})
    visiteur=public.new_page()
    # Aucun accès CDN dans le navigateur pour consulter les films déjà importés.
    public.route('https://**',lambda route: route.abort())
    visiteur.goto(base,wait_until='networkidle')
    charger_images(visiteur)
    visiteur.screenshot(path=str(sortie/'accueil-public.png'))
    resultats.append({'lecture_publique_sans_cdn':visiteur.locator('img').evaluate_all('(imgs)=>imgs.every(i=>i.complete && i.naturalWidth>0)')})
    visiteur.keyboard.press('Tab')
    resultats.append({'lien_evitement_clavier':visiteur.locator(':focus').inner_text()=='Aller au contenu'})
    public.close()
    resultats.append({'erreurs_js':erreurs})
    assert not erreurs,erreurs
    assert all(not r.get('debordement',False) and not r.get('images_invalides',[]) for r in resultats),resultats
    assert all(r.get('lecture_publique_sans_cdn',True) and r.get('lien_evitement_clavier',True) for r in resultats),resultats
    (sortie/'resultats.json').write_text(json.dumps(resultats,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(resultats,ensure_ascii=False,indent=2),flush=True)
    navigateur.close()
