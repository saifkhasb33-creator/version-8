import { Link } from 'react-router-dom';
import { useState, useEffect } from 'react';
import '../styles/landing.css';

import { 
  Truck, Users, Calendar, Wrench, Fuel, FileText, 
  BarChart3, Brain, Star, Mail, Phone, MapPin, 
  ChevronRight, Shield, Zap, Clock, Award, 
  TrendingUp, Globe, Headphones, CheckCircle,
  ArrowRight, Play, Menu, X, ExternalLink
} from 'lucide-react';

// Import des icônes sociales
import { FaInstagram, FaFacebook, FaYoutube } from 'react-icons/fa';

const LandingPage = () => {
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  // Images AGIL Energy (pour le carrousel uniquement)
  const agilImages = [
    { src: '/images/8JgQV.jpg', alt: 'AGIL Energy - L\'énergie qui propulse la Tunisie', title: 'L\'énergie qui propulse la Tunisie' },
    { src: '/images/yaNqP.jpg', alt: 'Innovation & Avenir - Dépôt de carburants de Goulette', title: 'Innovation & Avenir' },
    { src: '/images/ohPEe.jpg', alt: 'Qui est AGIL ? - Leader du marché tunisien', title: 'Leader du marché tunisien' },
    { src: '/images/iMdiE.jpg', alt: 'Découvrez AGIL ENERGY - Société Nationale de Distribution des Pétroles', title: 'AGIL Energy' }
  ];

  // URLs des réseaux sociaux AGIL
  const socialLinks = [
    { 
      name: 'Instagram', 
      url: 'https://www.instagram.com/agilenergytunisie/?hl=fr', 
      icon: FaInstagram, 
      color: 'hover:text-pink-500' 
    },
    { 
      name: 'Facebook', 
      url: 'https://www.facebook.com/agil.com.tn', 
      icon: FaFacebook, 
      color: 'hover:text-blue-500' 
    },
    { 
      name: 'YouTube', 
      url: 'https://www.youtube.com/@agilTun', 
      icon: FaYoutube, 
      color: 'hover:text-red-500' 
    }
  ];

  // Rotation automatique
  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentImageIndex((prev) => (prev + 1) % agilImages.length);
    }, 5000);
    return () => clearInterval(interval);
  }, []);

  // Statistiques AGIL Energy
  const agilStats = [
    { value: '216', label: 'Stations-service', icon: MapPin },
    { value: '54', label: 'Stations portuaires', icon: Globe },
    { value: '6', label: 'Aéroports', icon: Award },
    { value: '98%', label: 'Satisfaction client', icon: TrendingUp },
  ];

  // Fonctionnalités principales
  const features = [
    { icon: Truck, title: 'Gestion de flotte', desc: 'Suivez tous vos véhicules en temps réel', color: 'from-blue-500 to-cyan-500' },
    { icon: Users, title: 'Gestion chauffeurs', desc: 'Planifiez et suivez vos équipes', color: 'from-emerald-500 to-teal-500' },
    { icon: Calendar, title: 'Missions', desc: 'Organisez et optimisez vos missions', color: 'from-amber-500 to-orange-500' },
    { icon: Wrench, title: 'Maintenance', desc: 'Maintenance préventive par kilométrage', color: 'from-red-500 to-rose-500' },
    { icon: Fuel, title: 'Carburant', desc: 'Suivi consommation et cartes carburant', color: 'from-purple-500 to-indigo-500' },
    { icon: FileText, title: 'Documents', desc: 'Gestion documentaire avec OCR', color: 'from-pink-500 to-rose-500' },
    { icon: BarChart3, title: 'Statistiques', desc: 'Rapports et analyses détaillés', color: 'from-sky-500 to-blue-500' },
    { icon: Brain, title: 'IA intégrée', desc: 'OCR pour lecture automatique', color: 'from-violet-500 to-purple-500' },
  ];

  // Témoignages
  const testimonials = [
    { 
      name: 'Tunisie Telecom', 
      role: 'Directeur parc auto', 
      content: 'Agil a révolutionné notre gestion de flotte. Gain de temps : 40%', 
      rating: 5,
      logo: '🏢'
    },
    { 
      name: 'SOTRAPIL', 
      role: 'Responsable maintenance', 
      content: 'La maintenance préventive nous a fait économiser 25% sur nos coûts', 
      rating: 5,
      logo: '⚡'
    },
    { 
      name: 'STEG', 
      role: 'Chef de parc', 
      content: 'Interface intuitive et support réactif. Je recommande !', 
      rating: 5,
      logo: '💡'
    },
  ];

  // Avantages
  const benefits = [
    { icon: Zap, title: 'Rapide', desc: 'Réduction de 40% du temps de gestion', color: 'text-yellow-500' },
    { icon: Shield, title: 'Sécurisé', desc: 'Données chiffrées et sauvegardées', color: 'text-green-500' },
    { icon: Clock, title: '24/7', desc: 'Accès permanent à vos données', color: 'text-blue-500' },
    { icon: Award, title: 'Certifié', desc: 'Solution conforme aux normes', color: 'text-purple-500' },
  ];

  return (
    <div className="min-h-screen bg-black">
      {/* Navigation améliorée */}
      <nav className="fixed w-full bg-black/90 backdrop-blur-md border-b border-yellow-400/20 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center gap-2">
              <img src="/images/logo-agil.jpeg" alt="Agil" className="h-10 w-auto" />
              <span className="font-bold text-xl bg-gradient-to-r from-yellow-400 to-yellow-600 bg-clip-text text-transparent">
                Agil
              </span>
            </div>
            
            {/* Desktop Menu */}
            <div className="hidden md:flex items-center gap-6">
              {['Fonctionnalités', 'Témoignages', 'Contact'].map((item) => (
                <a key={item} href={`#${item.toLowerCase().replace(' ', '-')}`} className="text-gray-300 hover:text-yellow-400 transition">
                  {item}
                </a>
              ))}
              <Link to="/login" className="text-yellow-400 hover:text-yellow-300">Connexion</Link>
              <Link to="/register" className="bg-yellow-400 text-black px-5 py-2 rounded-full font-semibold hover:bg-yellow-500 transition shadow-lg shadow-yellow-400/20">
                Essai gratuit
              </Link>
            </div>

            {/* Mobile Menu Button */}
            <button onClick={() => setMobileMenuOpen(!mobileMenuOpen)} className="md:hidden text-yellow-400">
              {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
            </button>
          </div>

          {/* Mobile Menu */}
          {mobileMenuOpen && (
            <div className="md:hidden py-4 border-t border-yellow-400/20">
              {['Fonctionnalités', 'Témoignages', 'Contact'].map((item) => (
                <a key={item} href={`#${item.toLowerCase().replace(' ', '-')}`} className="block py-2 text-gray-300 hover:text-yellow-400">
                  {item}
                </a>
              ))}
              <Link to="/login" className="block py-2 text-yellow-400">Connexion</Link>
              <Link to="/register" className="block mt-2 bg-yellow-400 text-black text-center px-4 py-2 rounded-full font-semibold">Essai gratuit</Link>
            </div>
          )}
        </div>
      </nav>

      {/* Hero Section avec carrousel */}
      <section className="pt-24 pb-20 bg-gradient-to-br from-yellow-400 via-yellow-500 to-yellow-600">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            <div className="text-black">
              <span className="inline-block px-4 py-2 bg-black/10 rounded-full text-sm font-semibold mb-6 backdrop-blur-sm">
                🚀 La nouvelle génération de gestion de flotte
              </span>
              <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold mb-6 leading-tight">
                Gérez votre parc automobile 
                <span className="text-black"> intelligemment</span>
              </h1>
              <p className="text-lg text-black/80 mb-8 leading-relaxed">
                Agil vous offre une solution complète avec IA intégrée pour optimiser votre flotte, 
                réduire vos coûts et gagner en efficacité.
              </p>
              <div className="flex flex-wrap gap-4">
                <Link to="/register" className="group bg-black text-yellow-400 px-8 py-3 rounded-full font-semibold hover:bg-black/90 transition flex items-center gap-2 shadow-xl">
                  Commencer maintenant
                  <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition" />
                </Link>
                <a href="#features" className="border-2 border-black text-black px-8 py-3 rounded-full font-semibold hover:bg-black/10 transition">
                  Découvrir
                </a>
              </div>
            </div>

            {/* Carrousel amélioré */}
            <div className="relative">
              <div className="relative bg-black/20 backdrop-blur-sm rounded-2xl p-4 shadow-2xl">
                <div className="relative w-full h-[400px] md:h-[450px] overflow-hidden rounded-xl">
                  {agilImages.map((img, idx) => (
                    <div
                      key={idx}
                      className={`absolute inset-0 transition-all duration-1000 ${
                        idx === currentImageIndex ? 'opacity-100 scale-100' : 'opacity-0 scale-95'
                      }`}
                    >
                      <img src={img.src} alt={img.alt} className="w-full h-full object-contain" />
                      <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/80 to-transparent p-4">
                        <p className="text-white text-sm font-medium">{img.title}</p>
                      </div>
                    </div>
                  ))}
                </div>
                {/* Indicateurs */}
                <div className="absolute bottom-6 left-0 right-0 flex justify-center gap-2">
                  {agilImages.map((_, idx) => (
                    <button
                      key={idx}
                      onClick={() => setCurrentImageIndex(idx)}
                      className={`h-2 rounded-full transition-all ${
                        idx === currentImageIndex ? 'w-8 bg-yellow-400' : 'w-2 bg-white/50'
                      }`}
                    />
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Section AGIL Energy Stats */}
      <section className="py-16 bg-black border-b border-yellow-400/20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
            {agilStats.map((stat, idx) => (
              <div key={idx} className="text-center group">
                <div className="flex justify-center mb-3">
                  <stat.icon className="w-8 h-8 text-yellow-400 group-hover:scale-110 transition" />
                </div>
                <p className="text-3xl font-bold text-white mb-1">{stat.value}</p>
                <p className="text-gray-400 text-sm">{stat.label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Features */}
      <section id="fonctionnalités" className="py-20 bg-black">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">
              Tout ce dont vous avez besoin
            </h2>
            <p className="text-xl text-gray-400 max-w-2xl mx-auto">
              Une solution complète pour gérer l'ensemble de votre parc automobile
            </p>
          </div>
          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
            {features.map((feature, idx) => (
              <div key={idx} className="group p-6 bg-white/5 rounded-2xl border border-white/10 hover:border-yellow-400/50 hover:bg-white/10 transition-all duration-300">
                <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${feature.color} flex items-center justify-center mb-4 group-hover:scale-110 transition`}>
                  <feature.icon className="w-6 h-6 text-white" />
                </div>
                <h3 className="text-xl font-semibold text-white mb-2">{feature.title}</h3>
                <p className="text-gray-400">{feature.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Avantages */}
      <section className="py-16 bg-gradient-to-r from-yellow-400/10 to-transparent border-y border-yellow-400/20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid md:grid-cols-4 gap-6">
            {benefits.map((benefit, idx) => (
              <div key={idx} className="flex items-center gap-4 p-4 bg-white/5 rounded-xl">
                <benefit.icon className={`w-10 h-10 ${benefit.color}`} />
                <div>
                  <h3 className="font-semibold text-white">{benefit.title}</h3>
                  <p className="text-sm text-gray-400">{benefit.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Témoignages */}
      <section id="témoignages" className="py-20 bg-black">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-3xl md:text-4xl font-bold text-center text-white mb-12">
            Ils nous font confiance
          </h2>
          <div className="grid md:grid-cols-3 gap-8">
            {testimonials.map((t, idx) => (
              <div key={idx} className="p-6 bg-white/5 rounded-2xl border border-white/10 hover:border-yellow-400/30 transition">
                <div className="flex gap-1 mb-4">
                  {[...Array(t.rating)].map((_, i) => (
                    <Star key={i} className="w-5 h-5 fill-yellow-400 text-yellow-400" />
                  ))}
                </div>
                <p className="text-gray-300 mb-4 text-lg italic">"{t.content}"</p>
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-yellow-400/20 rounded-full flex items-center justify-center text-xl">
                    {t.logo}
                  </div>
                  <div>
                    <p className="font-semibold text-white">{t.name}</p>
                    <p className="text-sm text-gray-400">{t.role}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Final */}
      <section className="py-20 bg-gradient-to-r from-yellow-400 to-yellow-500">
        <div className="max-w-4xl mx-auto text-center px-4">
          <h2 className="text-3xl md:text-4xl font-bold text-black mb-4">
            Prêt à optimiser votre flotte ?
          </h2>
          <p className="text-black/80 text-lg mb-8">
            Rejoignez plus de 200 entreprises qui utilisent Agil au quotidien
          </p>
          <div className="flex flex-wrap justify-center gap-4">
            <Link to="/register" className="bg-black text-yellow-400 px-8 py-3 rounded-full font-semibold hover:bg-black/90 transition shadow-xl">
              Commencer maintenant
            </Link>
            <a href="#contact" className="border-2 border-black text-black px-8 py-3 rounded-full font-semibold hover:bg-black/10 transition">
              Nous contacter
            </a>
          </div>
        </div>
      </section>

      {/* Contact */}
      <section id="contact" className="py-20 bg-black">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid md:grid-cols-2 gap-12">
            <div>
              <h2 className="text-3xl font-bold text-yellow-400 mb-4">Contactez-nous</h2>
              <p className="text-gray-400 mb-8">Une question ? Notre équipe est là pour vous aider</p>
              <div className="space-y-4">
                <div className="flex items-center gap-3 text-gray-300">
                  <Mail className="w-5 h-5 text-yellow-400" />
                  <span>contact@agil.tn</span>
                </div>
                <div className="flex items-center gap-3 text-gray-300">
                  <Phone className="w-5 h-5 text-yellow-400" />
                  <span>+216 71 234 567</span>
                </div>
                <div className="flex items-center gap-3 text-gray-300">
                  <MapPin className="w-5 h-5 text-yellow-400" />
                  <span>Tunis, Tunisie</span>
                </div>
              </div>
            </div>
            <div className="bg-white/5 rounded-2xl p-8 border border-white/10">
              <form className="space-y-4">
                <input type="text" placeholder="Nom complet" className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder:text-gray-500 focus:outline-none focus:border-yellow-400" />
                <input type="email" placeholder="Email" className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder:text-gray-500 focus:outline-none focus:border-yellow-400" />
                <textarea rows="4" placeholder="Message" className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder:text-gray-500 focus:outline-none focus:border-yellow-400" />
                <button className="w-full bg-yellow-400 text-black px-6 py-3 rounded-lg font-semibold hover:bg-yellow-500 transition">
                  Envoyer
                </button>
              </form>
            </div>
          </div>
        </div>
      </section>

      {/* Footer avec icônes sociales */}
      <footer className="bg-black border-t border-yellow-400/20 py-8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col md:flex-row justify-between items-center gap-4">
            <div className="flex items-center gap-2">
              <img src="/images/logo-agil.jpeg" alt="Agil" className="h-8 w-auto" />
              <span className="font-bold text-lg text-yellow-400">Agil</span>
            </div>
            
            {/* Liens des réseaux sociaux */}
            <div className="flex items-center gap-4">
              {socialLinks.map((social) => (
                <a
                  key={social.name}
                  href={social.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className={`text-gray-400 hover:text-yellow-400 transition-all duration-300 hover:scale-110 ${social.color}`}
                  title={social.name}
                >
                  <social.icon className="w-6 h-6" />
                </a>
              ))}
            </div>
            
            <p className="text-gray-400 text-sm">© 2026 Agil. Tous droits réservés.</p>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default LandingPage;   