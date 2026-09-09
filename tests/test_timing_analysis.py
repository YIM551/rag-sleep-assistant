import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("timings", Path(__file__).parents[1] / "scripts/benchmark_retrieval.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class TimingAnalysisTest(unittest.TestCase):
    def line(self, total=20):
        return ("RAG_STAGE_TIMING {query_expansion_ms=2, retrieval_ms=3, reranking_ms=1, "
                f"prompt_build_ms=1, llm_ms=10, total_ms={total}" + "}")

    def test_synthetic_mean_and_nearest_rank(self):
        result = module.analyze([self.line(20), self.line(30)])
        self.assertEqual(result["stages"]["total_ms"], {"mean_ms": 25, "p95_ms": 30})

    def test_invalid_or_incomplete_records_are_rejected(self):
        for text in [self.line(float('nan')), self.line(2), self.line(-1),
                     self.line().replace('llm_ms=10, ', ''), self.line().replace('retrieval_ms=3', 'llm_ms=3')]:
            with self.subTest(text=text), self.assertRaises(ValueError):
                module.analyze([text])

    def test_no_records_is_not_a_zero_result(self):
        with self.assertRaises(ValueError):
            module.analyze(['unrelated log'])


if __name__ == '__main__':
    unittest.main()
