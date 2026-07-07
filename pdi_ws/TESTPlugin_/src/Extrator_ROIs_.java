import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import java.io.File;
import ij.plugin.ChannelSplitter;

public class Extrator_ROIs_ implements PlugIn {

    @Override
    public void run(String arg) {
        String dirEntrada = solicitarDiretorio("Selecione a pasta com as imagens ORIGINAIS");
        if (dirEntrada == null) return;

        String dirSaida = solicitarDiretorio("Selecione a pasta para salvar os recortes (ROIs)");
        if (dirSaida == null) return;

        File pastaEntrada = new File(dirEntrada);
        File[] arquivos = pastaEntrada.listFiles();

        if (arquivos == null || arquivos.length == 0) {
            IJ.error("Aviso", "A pasta de entrada parece estar vazia ou não pôde ser lida.");
            return;
        }

        // Prepara o RoiManager. Se ele já estiver aberto de um teste anterior, reaproveita.
        RoiManager roiManager = RoiManager.getInstance();
        if (roiManager == null) {
            roiManager = new RoiManager();
        }

        // Delega o trabalho em lote para um método específico
        processarLoteDeImagens(arquivos, dirSaida, roiManager);

        roiManager.close();
    }

    // ==========================================
    // GERENCIAMENTO
    // ==========================================
    private String solicitarDiretorio(String titulo) {
        DirectoryChooser dc = new DirectoryChooser(titulo);
        return dc.getDirectory();
    }

    private void processarLoteDeImagens(File[] arquivos, String dirSaida, RoiManager roiManager) {
        int imagensProcessadas = 0;
        int roisExtraidas = 0;

        for (File arquivo : arquivos) {
            // Garante que não vamos tentar processar pastas ocultas ou arquivos de texto
            if (arquivo.isFile() && arquivoEhImagem(arquivo.getName())) {
                roisExtraidas += processarImagemUnica(arquivo, dirSaida, roiManager);
                imagensProcessadas++;
            }
        }

        IJ.showMessage("Processamento finalizado!\n" +
                       "Imagens analisadas: " + imagensProcessadas + "\n" +
                       "Cones identificados e salvos: " + roisExtraidas);
    }

    // ==========================================
    // PROCESSAMENTO
    // ==========================================

    private int processarImagemUnica(File arquivo, String dirSaida, RoiManager roiManager) {
        ImagePlus imgOriginal = IJ.openImage(arquivo.getAbsolutePath());
        if (imgOriginal == null) return 0; // ignora se imagem corrompida

        ImagePlus mascara = imgOriginal.duplicate();

        mascara = isolarCanalAzul(mascara); // Levando em consideração cones preto e amarelos
        aplicarLimiarETratamentos(mascara);

        // Limpa as marcações do cone anterior antes de procurar os novos
        roiManager.reset();

        // "size=100-Infinity" evitar ruidos / "add" é pra adicionar as ROIs ao ROI manager
        IJ.run(mascara, "Analyze Particles...", "size=100-Infinity add");

        Roi[] roisEncontradas = roiManager.getRoisAsArray();
        String nomeBase = obterNomeSemExtensao(arquivo.getName());

        salvarRecortes(imgOriginal, roisEncontradas, dirSaida, nomeBase);

        // Fecha as janelas 
        imgOriginal.close();
        mascara.close();

        return roisEncontradas.length;
    }

    private ImagePlus isolarCanalAzul(ImagePlus img) {
    	
        ImagePlus[] canais = ChannelSplitter.split(img); //separar canais
        img.close(); 
        
        canais[0].close(); 
        canais[1].close();
        
        return canais[2];  // Fica apenas o canal Azul
    }

    private void aplicarLimiarETratamentos(ImagePlus img) {
    	
    	IJ.run(img, "8-bit", "");
    	
    	IJ.setAutoThreshold(img, "Default");
    	
        IJ.run(img, "Convert to Mask", "");
        
        IJ.run(img, "Fill Holes", "");
    }

    private void salvarRecortes(ImagePlus imgOriginal, Roi[] rois, String dirSaida, String nomeBase) {
        for (int i = 0; i < rois.length; i++) {
            // Coloca o contorno do cone detectado em cima da foto original
            imgOriginal.setRoi(rois[i]);
            
            // Recortar exatamente esse pedaço
            ImagePlus imgRecortada = imgOriginal.crop();
            
            // "fotoOriginal_cone_1.png"
            String caminhoFinal = dirSaida + File.separator + nomeBase + "_cone_" + (i + 1) + ".png"; // nome da imagem quando salva
            IJ.saveAs(imgRecortada, "PNG", caminhoFinal);
        }
    }

    // ==========================================
    // AUXILIARES
    // ==========================================

    private boolean arquivoEhImagem(String nome) {
    	String nomeMinusculo = nome.toLowerCase();
        
        String[] extensoesValidas = {".jpg", ".jpeg", ".png", ".tif", ".tiff"};
        
        for (String extensao : extensoesValidas) {
            if (nomeMinusculo.endsWith(extensao)) {
                return true;
            }
        }
        return false; 
    }

    private String obterNomeSemExtensao(String nomeArquivo) {
    	int indicePonto = nomeArquivo.lastIndexOf('.');
        
        if (indicePonto > 0) {
            return nomeArquivo.substring(0, indicePonto);
        } else {
            return nomeArquivo;
        }
    }
}