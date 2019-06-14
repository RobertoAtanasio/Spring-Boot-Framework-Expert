package com.algaworks.brewer.repository.helper.cerveja;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.algaworks.brewer.dto.CervejaDTO;
import com.algaworks.brewer.dto.ValorItensEstoque;
import com.algaworks.brewer.model.Cerveja;
import com.algaworks.brewer.repository.filter.CervejaFilter;
import com.algaworks.brewer.repository.ordenacao.Ordenacao;
import com.algaworks.brewer.repository.paginacao.Paginacao;
import com.algaworks.brewer.storage.FotoStorage;

public class CervejasImpl implements CervejasQueries {

	@PersistenceContext
	private EntityManager manager;
	
	@Autowired
	private Paginacao paginacao;
	
	@Autowired
	private Ordenacao ordenacao;
	
	@Autowired
	private FotoStorage fotoStorage;
	
	@Override
	@Transactional(readOnly = true)
	public Page<Cerveja> filtrar(CervejaFilter filtro, Pageable pageable) {
		
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		CriteriaQuery<Cerveja> criteria = builder.createQuery(Cerveja.class);
		Root<Cerveja> root = criteria.from(Cerveja.class);
		
		Predicate[] predicates = criarRestricoes(filtro, builder, root);
		criteria.where(predicates);
		
		ordenacao.ordenacao(pageable, builder, criteria, root);
		
		TypedQuery<Cerveja> query = manager.createQuery(criteria);
		
		paginacao.paginacao(pageable, query);
		
		Page<Cerveja> pagina = new PageImpl<>(query.getResultList(), pageable, total(filtro));
		
		manager.close();
		
		return pagina;
	}
	
	private Predicate[] criarRestricoes(CervejaFilter filtro, CriteriaBuilder builder,
			Root<Cerveja> root) {
		List<Predicate> predicates = new ArrayList<>();

		if (filtro != null) {
			if (!StringUtils.isEmpty(filtro.getSku())) {
				predicates.add(builder.equal(root.get("sku"), filtro.getSku()));
			}
			
			if (!StringUtils.isEmpty(filtro.getNome())) {
				predicates.add(builder.like(root.get("nome"), filtro.getNome()));
			}

			if (isEstiloPresente(filtro)) {
				predicates.add(builder.equal(root.get("estilo"), filtro.getEstilo()));
			}

			if (filtro.getSabor() != null) {
				predicates.add(builder.equal(root.get("sabor"), filtro.getSabor()));
			}

			if (filtro.getOrigem() != null) {
				predicates.add(builder.equal(root.get("origem"), filtro.getOrigem()));
			}

			if (filtro.getValorDe() != null) {
				predicates.add(builder.greaterThanOrEqualTo(root.get("valor"), filtro.getValorDe()));
			}

			if (filtro.getValorAte() != null) {
				predicates.add(builder.lessThanOrEqualTo(root.get("valor"), filtro.getValorAte()));
			}
		}
				
		return predicates.toArray(new Predicate[predicates.size()]);
	}
	
	@Override
	public List<CervejaDTO> porSkuOuNome(String skuOuNome) {
		String jpql = "select new com.algaworks.brewer.dto.CervejaDTO(codigo, sku, nome, origem, valor, foto) "
				+ "from Cerveja where lower(sku) like lower(:skuOuNome) or lower(nome) like lower(:skuOuNome)";
		List<CervejaDTO> cervejasFiltradas = manager.createQuery(jpql, CervejaDTO.class)
					.setParameter("skuOuNome", skuOuNome + "%")
					.getResultList();
		cervejasFiltradas.forEach(c -> c.setUrlThumbnailFoto(fotoStorage.getUrl(FotoStorage.THUMBNAIL_PREFIX + c.getFoto())));
		return cervejasFiltradas;
	}
	
	@Override
	public ValorItensEstoque valorItensEstoque() {
		String query = "select new com.algaworks.brewer.dto.ValorItensEstoque(sum(valor * quantidadeEstoque), sum(quantidadeEstoque)) from Cerveja";
		return manager.createQuery(query, ValorItensEstoque.class).getSingleResult();
	}
	
	private Long total(CervejaFilter filtro) {
		
		CriteriaBuilder builder = manager.getCriteriaBuilder();
		CriteriaQuery<Cerveja> criteria = builder.createQuery(Cerveja.class);
		Root<Cerveja> root = criteria.from(Cerveja.class);
		
		Predicate[] predicates = criarRestricoes(filtro, builder, root);
		criteria.where(predicates);
		
		TypedQuery<Cerveja> query = manager.createQuery(criteria);
		
		long quantidade = query.getResultList().size();
		
//		Criteria criteria = manager.unwrap(Session.class).createCriteria(Cerveja.class);
//		adicionarFiltro(filtro, criteria);
//		criteria.setProjection(Projections.rowCount());
//		return (Long) criteria.uniqueResult();
		
		return quantidade;
	}

//	private void adicionarFiltro(CervejaFilter filtro, Criteria criteria) {
//		if (filtro != null) {
//			if (!StringUtils.isEmpty(filtro.getSku())) {
//				criteria.add(Restrictions.eq("sku", filtro.getSku()));
//			}
//			
//			if (!StringUtils.isEmpty(filtro.getNome())) {
//				criteria.add(Restrictions.ilike("nome", filtro.getNome(), MatchMode.ANYWHERE));
//			}
//
//			if (isEstiloPresente(filtro)) {
//				criteria.add(Restrictions.eq("estilo", filtro.getEstilo()));
//			}
//
//			if (filtro.getSabor() != null) {
//				criteria.add(Restrictions.eq("sabor", filtro.getSabor()));
//			}
//
//			if (filtro.getOrigem() != null) {
//				criteria.add(Restrictions.eq("origem", filtro.getOrigem()));
//			}
//
//			if (filtro.getValorDe() != null) {
//				criteria.add(Restrictions.ge("valor", filtro.getValorDe()));
//			}
//
//			if (filtro.getValorAte() != null) {
//				criteria.add(Restrictions.le("valor", filtro.getValorAte()));
//			}
//		}
//	}
	
	private boolean isEstiloPresente(CervejaFilter filtro) {
		return filtro.getEstilo() != null && filtro.getEstilo().getCodigo() != null;
	}

}
