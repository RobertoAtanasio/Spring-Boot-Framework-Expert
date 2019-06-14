package com.algaworks.brewer.storage.local;

import static java.nio.file.FileSystems.getDefault;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.algaworks.brewer.storage.FotoStorage;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.name.Rename;

@Profile("!prod")
@Component
public class FotoStorageLocal implements FotoStorage {

	private static final Logger logger = LoggerFactory.getLogger(FotoStorageLocal.class);
	private static final String THUMBNAIL_PREFIX = "thumbnail.";
	
	@Value("${brewer.foto-storage-local.local}")
	private Path local;

	@Value("${brewer.foto-storage-local.local}")
	private String localString;
	
	// >>>>>> Path: local\.brewerfotos
	
	@Value("${brewer.foto-storage-local.url-base}")
	private String urlBase;
	
	// obs.: após a criação do parâmetro @Value("${brewer.foto-storage-local.local}"), não é necessário a criação 
	// do método construtor da classe, logo, será retirado.
	// O método criarPasta() será injetado com o atributo @PostConstruct a fim de que seja executado quando a classe
	// for instanciada.
	
//	public FotoStorageLocal() {
////		this(getDefault().getPath(System.getenv("HOME"), ".brewerfotos"));
//		this(getDefault().getPath("local", ".brewerfotos"));
//	}
//	
//	public FotoStorageLocal(Path path) {
//		this.local = path;
//		System.out.println(">>>>>> Path: " + this.local);
//		criarPastas();
//	}

	@Override
	public String salvar(MultipartFile[] files) {
		String novoNome = null;
		if (files != null && files.length > 0) {
			MultipartFile arquivo = files[0];
			novoNome = renomearArquivo(arquivo.getOriginalFilename());
			try {
//				System.out.println(">>> salvar: " + this.local.toAbsolutePath().toString() + getDefault().getSeparator() + novoNome);
				arquivo.transferTo(new File(this.local.toAbsolutePath().toString() + getDefault().getSeparator() + novoNome));
			} catch (IOException e) {
				throw new RuntimeException("Erro salvando a foto", e);
			}
		}
		
		try {
			Thumbnails.of(this.local.resolve(novoNome).toString()).size(40, 68).toFiles(Rename.PREFIX_DOT_THUMBNAIL);
		} catch (IOException e) {
			throw new RuntimeException("Erro gerando thumbnail", e);
		}
		
		return novoNome;
	}
	
	@Override
	public byte[] recuperar(String nome) {
//		System.out.println(">>>>>> FotoStorageLocal/recuperar: " + this.local);
		try {
			return Files.readAllBytes(this.local.resolve(nome));
		} catch (IOException e) {
			throw new RuntimeException("Erro lendo a foto", e);
		}
	}
	
	@Override
	public byte[] recuperarThumbnail(String fotoCerveja) {
		return recuperar(THUMBNAIL_PREFIX + fotoCerveja);
	}
	
	@Override
	public void excluir(String foto) {
		try {
			Files.deleteIfExists(this.local.resolve(foto));
			Files.deleteIfExists(this.local.resolve(THUMBNAIL_PREFIX + foto));
		} catch (IOException e) {
			logger.warn(String.format("Erro apagando foto '%s'. Mensagem: %s", foto, e.getMessage()));
		}
		
	}
	
	@Override
	public String getUrl(String foto) {
//		System.out.println(">>>>>> URL-BASE: " + this.urlBase);
//		System.out.println(">>>>>> localString: " + this.localString);
		return urlBase + foto;
	}
	
	@PostConstruct
	private void criarPastas() {
		try {
			Files.createDirectories(this.local);
//			System.out.println(">>>>>> criarPastas: " + this.local);
//			System.out.println(">>>>>> Pasta default: " + this.local.toAbsolutePath());
			
			if (logger.isDebugEnabled()) {
				logger.debug("Pastas criadas para salvar fotos.");
				logger.debug("Pasta default: " + this.local.toAbsolutePath());
			}
		} catch (IOException e) {
			throw new RuntimeException("Erro criando pasta para salvar foto", e);
		}
	}
	
}
