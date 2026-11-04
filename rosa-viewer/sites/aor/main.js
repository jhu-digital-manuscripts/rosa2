/**
 * The Archaeology of Reading site.
 *
 * Everything here is fixed for the AoR website. Deployment-specific URLs come
 * from window.rosaSiteEnv, set in index.html; the site definition is in site.config.js.
 */

import '../../src/site/site.css';
import './site.css';
import { createSiteViewer } from '../../src/site';
import site from './site.config';

createSiteViewer(site);
